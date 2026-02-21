package com.nyzg.user.controller;

import com.nyzg.common.Pair;
import com.nyzg.common.netobj.HttpResp;
import com.nyzg.common.ss_utils.JwtThreadSafe;
import com.nyzg.user.conf.JWTConf;
import com.nyzg.user.netobj.*;
import com.nyzg.user.service.DbService;
import com.nyzg.user.service.SmtpService;
import com.nyzg.user.service.TokenService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@Slf4j
public class LoginController {
    private final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");
    private final HttpResp ILLEGAL_MAIL_ADDR = new HttpResp(false, HttpResp.INVALID_USER_INPUT, "非法邮箱地址");
    private final HttpResp ILLEGAL_USER_NAME = new HttpResp(false, HttpResp.INVALID_USER_INPUT, "用户名的长度在1-32之间");
    private final HttpResp ILLEGAL_PASSWORD = new HttpResp(false, HttpResp.INVALID_USER_INPUT, "请填写密码/请使用程序的接口发送密码");
    private final HttpResp INVALID_MAIL_CODE = new HttpResp(false, HttpResp.INVALID_USER_INPUT, "验证码的格式不正确");
    private final HttpResp EMPTY_TOKEN = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "token为空");
    private final HttpResp EMPTY_DEVICE = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "deviceId为空");
    private final HttpResp EXPIRE_TOKEN = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "过期token");
    private final HttpResp INVALID_TOKEN = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "非法token");
    private final HttpResp INVALID_DEVICE_ID = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "deviceId不能为空");
    private final HttpResp MAIL_LOGIN_FAIL = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "无法进行邮箱登录");
    private final HttpResp PASSWORD_LOGIN_FAIL = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "无法进行密码登录");

    @Resource
    SmtpService smtpService;
    @Resource
    TokenService tokenService;
    @Resource
    DbService dbService;
    @Resource
    JWTConf jwtConf;

    @GetMapping("/login/test/connection")
    public HttpResp testConnection() {
        return new HttpResp(true, 0, "测试成功"+Thread.currentThread().getName());
    }

    //申请注册，此时仅发送验证码
    @PostMapping("/login/register/user")
    public HttpResp registerUser(@RequestBody MailVerifyReq req) {
        return this.getMailVerifyCode(req);
    }

    /**
     * 校验验证码，然后注册，返回登录凭证，如果用户存在就直接登录
     * 响应数据应该是一个完整的用户
     */
    @PostMapping("/login/register/verify/mail/code")
    public HttpResp registerVerifyMailCode(@RequestBody RegisterVerifyMailReq req) {
        return this.onNullWhenRegisterUserReqAcceptable(req).orElseGet(() -> {
            //校验验证码，如果为空，则验证成功
            return this.verifyMailCodeSuccessAtNull(req.getEmail(), req.getCode()).orElseGet(() -> {
                //注册用户
                HttpResp userInsertResp = dbService.insertUserSuccessReturnUser(req);
                if (!userInsertResp.isSuccess()) {
                    return userInsertResp;
                }
                TokenUser tokenUser = (TokenUser) userInsertResp.getContent();
                tokenUser = tokenService.getJwtToken(tokenUser).orElse(null);
                if (tokenUser == null) {
                    return new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "无法生成新登录token");
                }
                return new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "成功邮箱登录", tokenUser);
            });
        });
    }

    //登录获取验证码
    @PostMapping("/login/get/mail/verify/code")
    public HttpResp getMailVerifyCode(@RequestBody MailVerifyReq req) {
        if (this.isMailAddrIllegal(req.getMailAddr())) {
            return ILLEGAL_MAIL_ADDR;
        }
        return smtpService.sendMail(req.getMailAddr());
    }

    /**
     * 校验邮箱验证码登录，正确就返回登录凭证
     * 响应数据中包含完整用户
     */
    @PostMapping("/login/verify/mail/code")
    public HttpResp verifyMailCode(@RequestBody MailCodeVerifyReq req) {
        HttpResp idResp = dbService.getUserIdByEmailNullAtFail(req.getMail());
        return !idResp.isSuccess() ? idResp :
                this.verifyMailCodeSuccessAtNull(req.getMail(), req.getCode()).orElseGet(() -> {
                    TokenUser tokenUser = tokenService.getJwtToken(req.getMail(), (Long) idResp.getContent()).orElse(null);
                    if (tokenUser == null) {
                        return MAIL_LOGIN_FAIL;
                    }
                    return new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "成功邮箱登录", tokenUser);
                });
    }

    /**
     * 校验密码登录，正确就返回登录凭证
     * 响应数据中包含完整用户
     */
    @PostMapping("/login/verify/password")
    public HttpResp verifyPassword(@RequestBody PasswordVerifyReq req) {
        if (this.isMailAddrIllegal(req.getEmail())) {
            return ILLEGAL_MAIL_ADDR;
        }
        if (req.getSecretWord() == null || req.getSecretWord().length != 32) {
            return ILLEGAL_PASSWORD;
        }
        HttpResp idResp = dbService.getUserIdByEmailNullAtFail(req.getEmail());
        return !idResp.isSuccess() ? idResp :
                dbService.onNullWhenPasswordCorrect(req.getEmail(), req.getSecretWord()).orElseGet(() -> {
                    TokenUser tokenUser = tokenService.getJwtToken(req.getEmail(), (Long) idResp.getContent()).orElse(null);
                    if (tokenUser == null) {
                        return PASSWORD_LOGIN_FAIL;
                    }
                    return new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "成功密码登录", tokenUser);
                });
    }


    /**
     * 校验指纹密钥登录，正确就返回登录凭证
     * 响应数据中包含完整用户
     */
    @PostMapping("/login/verify/secret/key")
    public HttpResp verifySecret(@RequestBody KeyVerifyReq req) {
        if (this.isMailAddrIllegal(req.getEmail())) {
            return ILLEGAL_MAIL_ADDR;
        }
        if (req.getDeviceId() == null || req.getDeviceId().isEmpty()) {
            return EMPTY_DEVICE;
        }
        if (req.getToken() == null || req.getToken().isEmpty()) {
            return EMPTY_TOKEN;
        }
        return dbService.doKeyLogin(req);
    }

    @PostMapping("/login/verify/get/token")
    public HttpResp verifyAndGetNewToken(@RequestBody Token req) {
        if (req == null || req.getToken() == null || req.getToken().isEmpty()) {
            return EMPTY_TOKEN;
        }
        Pair<JwtThreadSafe.Status, Map<String, Object>> status = JwtThreadSafe.validateJwtTokenHMac(req.getToken(), jwtConf.getHsKey());
        if (status.getA() == JwtThreadSafe.Status.EXPIRE) {
            return EXPIRE_TOKEN;
        }
        Optional<String> newToken = tokenService.getJwtTokenFromLastNullAtFail(status.getB());
        if (newToken.isEmpty() || status.getA() == JwtThreadSafe.Status.FAIL) {
            return INVALID_TOKEN;
        }
        return new HttpResp(
                true, HttpResp.COMMON_SUCCESS_CODE, "", new Token(newToken.get())
        );
    }

    @PostMapping("/login/biometric/add")
    public HttpResp biometricAdd(@RequestBody BiometricAddReq req) {
        if (isMailAddrIllegal(req.getEmail())) {
            return ILLEGAL_MAIL_ADDR;
        }
        if (req.getDeviceId() == null || req.getDeviceId().isEmpty()) {
            return INVALID_DEVICE_ID;
        }
        if (req.getSecretWord() == null || req.getSecretWord().length != 32) {
            return ILLEGAL_PASSWORD;
        }
        if (req.getDeviceId().length() > 32) {
            req.setDeviceId(req.getDeviceId().substring(0, 32));
        }
        if (req.getDeviceName() != null && req.getDeviceName().length() > 32) {
            req.setDeviceName(req.getDeviceName().substring(0, 32));
        } else {
            req.setDeviceName("未知设备");
        }
        return dbService.addTrustDevice(req);
    }

    private Optional<HttpResp> verifyMailCodeSuccessAtNull(String email, String code) {
        if (this.isMailAddrIllegal(email)) {
            return Optional.of(ILLEGAL_MAIL_ADDR);
        }
        if (code == null || code.length() != 6 || !code.matches("\\d+")) {
            return Optional.of(INVALID_MAIL_CODE);
        }
        return smtpService.onNullWhenMailCodeVerifySuccess(email, code);
    }

    private Optional<HttpResp> onNullWhenRegisterUserReqAcceptable(RegisterUserReq req) {
        if (this.isMailAddrIllegal(req.getEmail())) {
            return Optional.of(ILLEGAL_MAIL_ADDR);
        }
        if (req.getNickName() == null || req.getNickName().length() > 32) {
            return Optional.of(ILLEGAL_USER_NAME);
        }
        //密码是sha-256哈希过的，应该是32byte
        if (req.getSecretWord() == null || req.getSecretWord().length != 32) {
            return Optional.of(ILLEGAL_PASSWORD);
        }
        return Optional.empty();
    }

    private boolean isMailAddrIllegal(String mailAddr) {
        if (mailAddr == null) {
            return true;
        }
        return !EMAIL_PATTERN.matcher(mailAddr).matches();
    }
}
