package com.nyzg.user.controller;

import com.nyzg.common.Pair;
import com.nyzg.common.ss_utils.JwtThreadSafe;
import com.nyzg.user.conf.JWTConf;
import com.nyzg.user.netobj.*;
import com.nyzg.user.remoteobj.RemoteUser;
import com.nyzg.user.service.DbService;
import com.nyzg.user.service.SmtpService;
import com.nyzg.user.service.TokenService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
public class LoginController {
    private final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");
    private final HttpResp ILLEGAL_MAIL_ADDR = new HttpResp(false, HttpResp.INVALID_USER_INPUT, "非法邮箱地址");
    private final HttpResp ILLEGAL_USER_NAME = new HttpResp(false, HttpResp.INVALID_USER_INPUT, "用户名的长度在1-32之间");
    private final HttpResp ILLEGAL_PASSWORD = new HttpResp(false, HttpResp.INVALID_USER_INPUT, "请填写密码/请使用程序的接口发送密码");
    private final HttpResp INVALID_MAIL_CODE = new HttpResp(false, HttpResp.INVALID_USER_INPUT, "验证码的格式不正确");
    private final HttpResp INVALID_SECRET_KEY = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "密钥验签不能为空");
    private final HttpResp EMPTY_TOKEN = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "token为空");
    private final HttpResp EXPIRE_TOKEN = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "过期token");
    private final HttpResp INVALID_TOKEN = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "非法token");

    @Resource
    SmtpService smtpService;
    @Resource
    TokenService tokenService;
    @Resource
    DbService dbService;
    @Resource
    JWTConf jwtConf;

    //申请注册，此时仅发送验证码
    @PostMapping("/register/user")
    public HttpResp registerUser(@RequestBody MailVerifyReq req) {
        return this.getMailVerifyCode(req);
    }

    //校验验证码，然后注册，返回登录凭证，如果用户存在就直接登录
    //响应数据应该是一个完整的用户
    @PostMapping("/register/verify/mail/code")
    public HttpResp registerVerifyMailCode(@RequestBody RegisterVerifyMailReq req) {
        return this.onNullWhenRegisterUserReqAcceptable(req).orElseGet(() -> {
            //校验验证码，如果为空，则验证成功
            return this.verifyMailCodeSuccessAtNull(req.getEmail(), req.getCode()).orElseGet(() -> {
                //注册用户
                HttpResp userInsertResp = dbService.insertUserSuccessReturnUser(req);
                if (!userInsertResp.isSuccess()) {
                    return userInsertResp;
                }
                RemoteUser remoteUser = (RemoteUser) userInsertResp.getContent();
                Token token = tokenService.getJwtToken(req.getEmail(), remoteUser.getId());
                remoteUser.setToken(token.getToken());
                return new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "成功邮箱登录", remoteUser);
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

    //校验邮箱验证码登录，正确就返回登录凭证
    @PostMapping("/login/verify/mail/code")
    public HttpResp verifyMailCode(@RequestBody MailCodeVerifyReq req) {
        HttpResp idResp = dbService.getUserIdByEmailNullAtFail(req.getMail());
        return !idResp.isSuccess() ? idResp :
                this.verifyMailCodeSuccessAtNull(req.getMail(), req.getCode()).orElse(
                        new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "成功邮箱登录",
                                tokenService.getJwtToken(req.getMail(), (Long) idResp.getContent())
                        )
                );
    }

    //校验密码登录，正确就返回登录凭证
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
                dbService.onNullWhenPasswordCorrect(req.getEmail(), req.getSecretWord()).orElse(
                        new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "成功密码登录",
                                tokenService.getJwtToken(req.getEmail(), (Long) idResp.getContent())
                        )
                );
    }

    //指纹密钥登录，返回challenge
    @PostMapping("/login/get/secret/challenge")
    public HttpResp getLoginSecretChallenge(@RequestBody SecretChallengeReq req) {
        if (this.isMailAddrIllegal(req.getEmail())) {
            return INVALID_MAIL_CODE;
        }
        return dbService.getSecretChallenge(req.getEmail());
    }

    //校验指纹密钥登录，正确就返回登录凭证
    @PostMapping("/login/verify/secret/key")
    public HttpResp verifySecret(@RequestBody SecretVerifyReq req) {
        if (this.isMailAddrIllegal(req.getEmail())) {
            return ILLEGAL_MAIL_ADDR;
        }
        if (req.getSign() == null || req.getSign().isEmpty()) {
            return INVALID_SECRET_KEY;
        }
        HttpResp idResp = dbService.getUserIdByEmailNullAtFail(req.getEmail());
        return !idResp.isSuccess() ? idResp :
                dbService.onNullWhenSecretChallengeSucceed(req.getEmail(), req.getSign())
                        .orElse(new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "成功密钥登录",
                                tokenService.getJwtToken(req.getEmail(), (Long) idResp.getContent())
                        ));
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
