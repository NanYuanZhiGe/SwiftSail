package com.nyzg.dock.controller;

import com.nyzg.common.Pair;
import com.nyzg.common.netobj.HttpResp;
import com.nyzg.common.ss_utils.EncryptThreadSafe;
import com.nyzg.common.ss_utils.Tuple;
import com.nyzg.dock.dbobj.Watch;
import com.nyzg.dock.mapper.WatchTableMapper;
import com.nyzg.dock.netobj.WatchAddReq;
import com.nyzg.dock.netobj.WatchDeleteReq;
import com.nyzg.dock.netobj.WatchGetResp;
import com.nyzg.dock.service.QuartzService;
import com.nyzg.dock.service.WatchService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.nyzg.dock.controller.FinalHttpResp.*;

/**
 * 这个类的主要作用是完成手表状态机的转换。
 * 用户的手表一共有三种状态：未添加，有效和无效，
 * 未添加：数据库中不存在这个手表，界定方式为userId+clientId
 * 有效：数据库中存在这个手表，且expireTime>currentTime && activate>0
 * 无效：数据库中存在这个手表，但是expireTime<=currentTime || activate <=0
 * 其中有四种转化关系：
 * 授权：任何状态转化到有效状态
 * 删除：任何状态转化到未添加的状态
 * 下线：有效状态转化为无效状态
 * X宕机：有效状态转化为无效状态
 * 其中，宕机只能由物理方式导致，比如网络问题，物理机关机等等
 * 然后，授权分为两步，厂商授权和授权检查，只有完成了两步，才能够算是进行了授权
 */
@RestController
@Slf4j
public class FitbitCallbackController {
    private final static HttpResp SECOND_STATE_NOT_MATCH = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "第二次提交的关键信息和授权时填写的不一致！");
    @Resource
    WatchService watchService;
    @Resource
    QuartzService quartzService;
    @Resource
    WatchTableMapper watchTableMapper;

    /**
     * 用于测试使用，获取特定编码的字符串
     *
     * @return 返回一个三元组，原始随机字符串，sha256过后的哈希字符串，base64编码的哈希字符串
     */
    @GetMapping("/oauth/code")
    public Tuple<String, String, String> getCode() {
        return EncryptThreadSafe.getBase64UrlSha256RandomString();
    }

    /**
     * @return content为WatchGetResp
     */
    @PostMapping(value = "/get/watchList",produces = "application/json")
    public HttpResp getWatchList(@RequestHeader("userId") String userId) {
        log.info("getting");
        List<Watch> watchList = watchTableMapper.selectWatchByUserId(Long.parseLong(userId));
        return new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "", new WatchGetResp(watchList));
    }

    /**
     * 对应状态机中的删除，这个方法幂等
     *
     * @return 返回的content为void
     */
    @PostMapping("/watch/delete")
    public HttpResp watchDelete(@RequestHeader("userId") String userId, @RequestBody WatchDeleteReq req) {
        if (userId == null) {
            return LACK_USER_ID;
        }
        if (req.getClientId() == null || req.getClientId().isBlank()) {
            return HttpResp.COMMON_SUCCESS;
        }
        quartzService.stopSchedulerAndDelete(Long.parseLong(userId), req.getClientId());
        return HttpResp.COMMON_SUCCESS;
    }

    /**
     * 对应状态机中的下线
     * 会停止定时任务，然后设置这个手表的信息为expireTime=0,activate=0
     *
     * @return 返回的content为void
     */
    @PostMapping("/go/offline")
    public HttpResp goOffline(@RequestHeader("userId") String userId, @RequestBody WatchDeleteReq req) {
        if (userId == null) {
            return LACK_USER_ID;
        }
        if (req.getClientId() == null || req.getClientId().isBlank()) {
            return HttpResp.COMMON_SUCCESS;
        }
        //停止任务
        quartzService.stopSchedulerOnly(req.getClientId());
        //删除数据库中的表
        watchTableMapper.updateWatchOffline(Long.parseLong(userId), req.getClientId());
        return HttpResp.COMMON_SUCCESS;
    }

    /**
     * 状态机——授权：第二步，授权检查
     *
     * @param userId 这个是从网关jwt解析出来的
     * @param req    包含：userId,clientId,name,watchType,authorizeHeader,codeVerifier,state
     * @return 响应，content为void
     */
    @PostMapping("/is/watch/added")
    public HttpResp isWatchAdded(@RequestHeader("userId") String userId, @RequestBody WatchAddReq req) {
        //----------------------输入参数校验----------------------
        if (req.userId != Double.parseDouble(userId)) return USER_ID_NOT_MATCH_TOKEN;
        if (req.state == null) return STATE_IS_NULL;
        if (req.getUserId() == 0L) return LACK_USER_ID;
        if (req.getClientId() == null || req.getClientId().isEmpty()) return CLIENT_ID_EMPTY;
        if (req.getCodeVerifier() == null || req.getCodeVerifier().isEmpty() || req.getCodeVerifier().length() > 128 || req.getCodeVerifier().length() < 43)
            return CODE_VERIFIER_INVALID;
        if (req.getWatchType() == null) return UNKNOWN_DEVICE;
        if (req.getName() == null)//如果名字为空，就给一个默认的名字
            req.setName("我的设备");
        String stateVerifier = EncryptThreadSafe.getBase64UrlSha256WithCertainString(String.format("%d:%s:%s:%s", req.userId, req.clientId, req.watchType, req.authorizeHeader));
        if (!stateVerifier.equals(req.state)) {//检查第二次提交的关键信息是否和第一次提交的一致
            return SECOND_STATE_NOT_MATCH;
        }
        //++++++++++++++++++++++业务逻辑++++++++++++++++++++++++++++++
        //检查用户是否授权，如果成功，就会把这个手表写入数据库
        //这个操作是幂等的，“未添加”和“无效”两个状态都能够有效转化到“有效”这个状态
        Pair<HttpResp, String> httpRespPair = watchService.checkIsCallbackSuccessAndDoDatabase(req);
        if (!httpRespPair.getA().isSuccess()) {//如果不成功
            /*
            由两种可能，一种是授权码过期了，或者是写入数据库失败什么的，这种情况下这个手表是不会写到数据库里面的
            第二种是这个手表会被写入到数据中，但是由于当前由正在工作的手表，所以它的activate=false
             */
            return httpRespPair.getA();
        }
        //如果成功，就添加定时任务到quartz里面
        //这里面会先拉取一次Token
        return quartzService.addFitbitPullSchedule(req.userId, req.clientId, req.authorizeHeader, httpRespPair.getB(),//用户添加的授权码
                req.codeVerifier);
    }

    /**
     * 状态机——授权：第一步，厂商授权的回调
     * 厂商授权，用户点击授权按钮，跳转至浏览器，此时会进入厂商的界面进行授权，
     * 授权结束后会回调我们的这个接口，此时我们会得到授权码和一个用于验证的state
     * 后面用户进行授权检查的时候，我们会验证用户提交的信息是否和state一致来判定用户两次提交的
     * 信息是否一致
     *
     * @param state state由userId，clientId，手表的type等关键信息的哈希码组成，sha256后按照base64Url进行编码
     * @param code  授权码，我们需要这个授权码初次获取accessToken
     * @return html页面，告诉用户回调是否成功
     */
    @GetMapping("/oauth/callback/fitbit")
    public String fitbitCallback(@RequestParam(value = "state", required = false) String state, @RequestParam(value = "code", required = false) String code) {

        if (state == null || state.trim().isEmpty()) {
            return """
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <div style="font-family: sans-serif; text-align: center; padding: 30px; color: #e74c3c;">
                        😭 缺失授权参数（state）<br>
                        请返回 App 重新尝试。
                    </div>
                    """;
        }
        if (code == null || code.trim().isEmpty()) {
            return """
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <div style="font-family: sans-serif; text-align: center; padding: 30px; color: #e74c3c;">
                        😭 未获取到授权码<br>
                        您可能取消了授权，请重试。
                    </div>
                    """;
        }

        try {
            watchService.saveStateAndCode(state.trim(), code.trim());
        } catch (Exception e) {
            log.error("保存 Fitbit 授权失败", e);
            return """
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <div style="font-family: sans-serif; text-align: center; padding: 30px; color: #e74c3c;">
                        😶‍🌫️ 服务器出错了<br>
                        请稍后再试。
                    </div>
                    """;
        }
        return """
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <div style="font-family: sans-serif; text-align: center; padding: 30px; color: #27ae60;">
                    🙂 Fitbit 授权成功！<br>
                    请返回 App 点击“我已授权”完成绑定（60分钟内有效）。
                </div>
                """;
    }
}