package com.nyzg.user.service;

import cn.hutool.core.date.DateUtil;
import com.nyzg.common.Pair;
import com.nyzg.common.TimeUtils;
import com.nyzg.common.netobj.HttpResp;
import com.nyzg.common.ss_utils.JwtThreadSafe;
import com.nyzg.user.conf.JWTConf;
import com.nyzg.user.dbobj.TrustDevice;
import com.nyzg.user.dbobj.User;
import com.nyzg.user.mapper.TrustDeviceTableMapper;
import com.nyzg.user.mapper.UserTableMapper;
import com.nyzg.user.netobj.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DbService {
    private final HttpResp SERVER_CANNOT_WRITE_DB = new HttpResp(false, HttpResp.SERVER_INTERNAL_ERROR, "服务端无法写入数据");
    private final HttpResp NICK_NAME_SHOULD_UNIQUE = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "用户名已被其他用户使用");
    private final HttpResp EMAIL_NOT_EXISTS = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "用户不存在");
    private final HttpResp CHECK_PWD_OR_LOGIN = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "密码不正确或该账号未注册");
    private final HttpResp PASSWORD_NOT_CORRECT = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "密码不正确");
    private final HttpResp DEVICE_NOT_REGISTER = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "该设备未注册生物识别验证");
    private final HttpResp TOKEN_NOT_VALIDATE = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "凭证已过期，需要密码或验证码登录");
    @Resource
    IdService idService;
    @Resource
    UserTableMapper userTableMapper;
    @Resource
    TrustDeviceTableMapper trustDeviceTableMapper;
    @Resource
    JWTConf jwtConf;

    /**
     * 首先是确保用户存在，同时进行密码校验
     * 如果通过就添加这个设备到信任设备表中，生成一个token返回给客户端
     */
    public HttpResp addTrustDevice(BiometricAddReq req) {
        List<User> rows = userTableMapper.isHaveEmailAndSecretWord(req.getEmail(), req.getSecretWord());
        if (rows.isEmpty()) {
            return CHECK_PWD_OR_LOGIN;
        }
        User user = rows.get(0);
        TrustDevice trustDevice = new TrustDevice();
        trustDevice.setDeviceName(req.getDeviceName());
        trustDevice.setDeviceId(req.getDeviceId());
        trustDevice.setEmail(req.getEmail());
        try {
            trustDeviceTableMapper.insertDeviceInTable(trustDevice);
        } catch (Exception e) {
            return SERVER_CANNOT_WRITE_DB;
        }
        BiometricAddResp resp = new BiometricAddResp();
        resp.setToken(getTrustDeviceToken(
                user.getEmail(), user.getId()
        ));
        return new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "", resp);
    }

    public HttpResp insertUserSuccessReturnUser(RegisterUserReq req) {
        List<User> userList = userTableMapper.getUserByEmail(req.getEmail());
        if (!userList.isEmpty()) {//如果email存在，直接成功登录
            User user = userList.get(0);
            long createTime = TimeUtils.toEpochDays(user.getCreateTime());
            return new HttpResp(true, new TokenUser(user.getId(), user.getNickName(), user.getEmail(), createTime));
        }
        //用户不存在，需要执行插入语句
        //保证用户名不重复
        if (userTableMapper.isNickNameExists(req.getNickName()) != 0) {
            return NICK_NAME_SHOULD_UNIQUE;
        }
        User user = new User(
                idService.getId(),//线程安全
                req.getNickName(),
                req.getEmail(),
                req.getSecretWord()
        );
        try {
            userTableMapper.insertUserIfNotExists(user);
        } catch (Exception e) {
            return SERVER_CANNOT_WRITE_DB;
        }
        long createTime = TimeUtils.toEpochDays(user.getCreateTime());
        TokenUser safeUser = new TokenUser(user.getId(), user.getNickName(), user.getEmail(), createTime);
        return new HttpResp(true, safeUser);
    }

    public Optional<HttpResp> onNullWhenPasswordCorrect(String email, byte[] secretWord) {
        try {
            int rows = userTableMapper.isEmailExists(email);
            if (rows == 0) {
                return Optional.of(EMAIL_NOT_EXISTS);
            }
            List<User> userList = userTableMapper.isHaveEmailAndSecretWord(email, secretWord);
            if (userList.isEmpty()) {
                return Optional.of(PASSWORD_NOT_CORRECT);
            }
        } catch (Exception e) {
            return Optional.of(SERVER_CANNOT_WRITE_DB);
        }
        return Optional.empty();
    }

    public HttpResp doKeyLogin(KeyVerifyReq req) {
        List<User> user = userTableMapper.getUserByEmail(req.getEmail());
        if (user.isEmpty()) {//如果用户不存在
            return EMAIL_NOT_EXISTS;
        }
        User trueUser = user.get(0);
        //检查用户和设备是否注册过
        int rows = trustDeviceTableMapper.isUserDeviceInTable(req.getEmail(), req.getDeviceId());
        if (rows == 0) {
            return DEVICE_NOT_REGISTER;
        }
        //检查token是否过期
        Pair<JwtThreadSafe.Status, Map<String, Object>> statusMapPair = JwtThreadSafe.validateJwtTokenHMac(req.getToken(), jwtConf.getHsKey());
        if (statusMapPair.getA() != JwtThreadSafe.Status.SUCCESS) {
            return TOKEN_NOT_VALIDATE;
        }
        //生成两个token，一个是生物识别验证的token，一个是登录的token
        String tokenCommon = JwtThreadSafe.getJwtTokenHMac(
                Map.of("email", trueUser.getEmail(), "userId", trueUser.getId()),
                DateUtil.date(),
                DateUtil.date(),
                24,
                jwtConf.getHsKey()
        );
        String tokenBiometric = getTrustDeviceToken(trueUser.getEmail(), trueUser.getId());
        BiometricUser resUser = new BiometricUser();
        long createTime = TimeUtils.toEpochDays(trueUser.getCreateTime());
        TokenUser tokenUser = new TokenUser(
                trueUser.getId(), trueUser.getNickName(), trueUser.getEmail(), tokenCommon, createTime
        );
        resUser.setTokenUser(tokenUser);
        resUser.setToken(tokenBiometric);
        return new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "", resUser);
    }

    //根据email获取用户的id
    public HttpResp getUserIdByEmailNullAtFail(String email) {
        try {
            Long id = userTableMapper.getUserIdByEmail(email);
            if (id == null) {
                return EMAIL_NOT_EXISTS;
            }
            return new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "", id);
        } catch (Exception e) {
            return SERVER_CANNOT_WRITE_DB;
        }
    }

    private String getTrustDeviceToken(String email, long userId) {
        return JwtThreadSafe.getJwtTokenHMac(
                Map.of("email", email, "userId", userId),
                DateUtil.date(),
                DateUtil.date(),
                912,
                jwtConf.getHsKey()
        );
    }
}