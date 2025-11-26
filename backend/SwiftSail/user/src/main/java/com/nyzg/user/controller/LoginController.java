package com.nyzg.user.controller;

import com.nyzg.user.netobj.*;
import com.nyzg.user.service.DbService;
import com.nyzg.user.service.SmtpService;
import com.nyzg.user.service.TokenService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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

    @Resource
    SmtpService smtpService;
    @Resource
    TokenService tokenService;
    @Resource
    DbService dbService;

    //申请注册，此时仅发送验证码
    @PostMapping("/register/user")
    public HttpResp registerUser(@RequestBody RegisterUserReq req) {
        return this.getMailVerifyCode(new MailVerifyReq(req.getEmail()));
    }

    //校验验证码，然后注册，返回登录凭证，如果用户存在就直接登录
    @PostMapping("/register/verify/mail/code")
    public HttpResp registerVerifyMailCode(@RequestBody RegisterVerifyMailReq req) {
        return this.onNullWhenRegisterUserReqAcceptable(req).orElseGet(() -> {
            //校验验证码
            HttpResp resp = this.verifyMailCode(new MailCodeVerifyReq(req.getEmail(), req.getCode()));
            if (!resp.isSuccess()) {//如果失败
                return resp;
            }
            //如果成功就把数据写到数据库中
            return dbService.onNullWhenUserInserted(req).orElse(
                    new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "成功邮箱登录", tokenService.getJwtTokenResp(req.getEmail()))
            );
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
        if (this.isMailAddrIllegal(req.getMail())) {
            return ILLEGAL_MAIL_ADDR;
        }
        if (req.getCode() == null || req.getCode().length() != 6) {
            return INVALID_MAIL_CODE;
        }
        return smtpService.onNullWhenMailCodeVerifySuccess(req.getMail(), req.getCode()).orElse(
                new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "成功邮箱登录", tokenService.getJwtTokenResp(req.getMail()))
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
        return dbService.onNullWhenPasswordCorrect(req.getEmail(), req.getSecretWord()).orElse(
                new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "成功密码登录", tokenService.getJwtTokenResp(req.getEmail()))
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
        return dbService.onNullWhenSecretChallengeSucceed(req.getEmail(), req.getSign())
                .orElse(new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "成功密钥登录", tokenService.getJwtTokenResp(req.getEmail())));
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
