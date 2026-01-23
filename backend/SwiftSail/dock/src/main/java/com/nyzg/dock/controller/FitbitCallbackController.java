package com.nyzg.dock.controller;

import com.nyzg.common.Pair;
import com.nyzg.common.netobj.HttpResp;
import com.nyzg.common.ss_utils.EncryptThreadSafe;
import com.nyzg.common.ss_utils.Tuple;
import com.nyzg.dock.netobj.WatchAddReq;
import com.nyzg.dock.service.QuartzService;
import com.nyzg.dock.service.WatchService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
public class FitbitCallbackController {
    private final static HttpResp CLIENT_ID_EMPTY = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "clientId不能为空");
    private final static HttpResp CODE_VERIFIER_INVALID = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "非法code verifier");
    private final static HttpResp STATE_IS_NULL = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "您还没有授权，state为空");
    private final static HttpResp UNKNOWN_DEVICE = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "未知设备");
    private final static HttpResp LACK_USER_ID = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "缺少登录用户id");
    @Resource
    WatchService watchService;
    @Resource
    QuartzService quartzService;

    @GetMapping("/oauth/code")
    public Tuple<String, String, String> getCode() {
        return EncryptThreadSafe.getBase64UrlSha256RandomString();
    }

    /**
     * 客户端向服务端查询数据库有无添加到手表信息
     */
    @PostMapping("/is/watch/added")
    public HttpResp isWatchAdded(@RequestBody WatchAddReq req) {
        if (req.state == null)
            return STATE_IS_NULL;
        if (req.getUserId() == 0L)
            return LACK_USER_ID;
        if (req.getClientId() == null || req.getClientId().isEmpty())
            return CLIENT_ID_EMPTY;
        if (req.getCodeVerifier() == null || req.getCodeVerifier().isEmpty()
                || req.getCodeVerifier().length() > 128 || req.getCodeVerifier().length() < 43)
            return CODE_VERIFIER_INVALID;
        if (req.getWatchType() == null)
            return UNKNOWN_DEVICE;
        if (req.getName() == null)
            req.setName("我的设备");
        //检查用户是否授权
        Pair<HttpResp, String> httpRespPair = watchService.checkIsCallbackSuccessAndDoDatabase(req);
        if (!httpRespPair.getA().isSuccess()) {//如果不成功
            return httpRespPair.getA();
        }
        //如果成功，就添加定时任务到quartz里面
        quartzService.addFitbitPullSchedule(
                req.clientId, req.authorizeHeader,
                httpRespPair.getB(), req.codeVerifier
        );
        return httpRespPair.getA();
    }

    @GetMapping("/oauth/callback/fitbit")
    public String fitbitCallback(
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "code", required = false) String code) {

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