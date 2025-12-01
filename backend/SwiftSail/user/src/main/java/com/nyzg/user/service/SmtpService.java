package com.nyzg.user.service;

import com.nyzg.user.conf.SmtpConf;
import com.nyzg.user.netobj.HttpResp;
import jakarta.annotation.Resource;
import jakarta.mail.internet.MimeMessage;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

@Service
public class SmtpService {
    private final HttpResp SUCCESS_MAIL_SENT = new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "验证码已成功发送");
    private final HttpResp MAIL_HAVE_SENT = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "邮件已经发送，请5分钟后尝试重新发送");
    private final Duration MAIL_EXPIRE_TIME = Duration.ofSeconds(30 * 60);
    private final Duration MAIL_RESENT_TIME = Duration.ofSeconds(5 * 60);
    private final int MAIL_NOT_SENT = -1, MAIL_CAN_RESENT = 0, MAIL_CANNOT_RESENT = 1;
    private final HttpResp CODE_HAVE_EXPIRED = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "验证码已过期");
    private final HttpResp CODE_NOT_CORRECT = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "验证码不正确");
    private final HttpResp SERVER_UNABLE_SEND_MAIL = new HttpResp(false, HttpResp.SERVER_INTERNAL_ERROR, "服务端无法发送邮件");
    private final String MAIL_VERIFY_KEY = "user.verify.mail.%s";
    @Resource
    SmtpConf smtpConf;
    @Resource
    JavaMailSender mailSender;
    @Resource
    RedissonClient redissonClient;

    public HttpResp sendMail(String mailToReceive) {
        if (this.isMailHaveSent(mailToReceive) == MAIL_CANNOT_RESENT) {
            return MAIL_HAVE_SENT;
        }
        //发送邮件
        String code = this.getVerifyCode();
        String subject = "【SwiftSail 运动轻舟】 邮箱验证码";
        String htmlContent = """
                <div style="font-family: Arial, sans-serif; padding: 16px; border: 1px solid #eee;">
                    <h2>您好！</h2>
                    <p>您的验证码是：<strong style="color: #e74c3c; font-size: 16px;">%s</strong></p>
                    <p>有效期：30分钟</p>
                    <p>请勿泄露给他人。</p>
                </div>
                """.formatted(code);
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            messageHelper.setFrom(smtpConf.getMailSender());
            messageHelper.setTo(mailToReceive);
            messageHelper.setSubject(subject);
            messageHelper.setText(htmlContent, true);//html格式，指定为true
            mailSender.send(mimeMessage);
            //写入redis
            writeMailInRedis(mailToReceive, code);
        } catch (Exception e) {
            return SERVER_UNABLE_SEND_MAIL;
        }
        return SUCCESS_MAIL_SENT;
    }

    public Optional<HttpResp> onNullWhenMailCodeVerifySuccess(String mail, String code) {
        RScript onDeleteWhenCorrect = redissonClient.getScript(StringCodec.INSTANCE);
        Long result=onDeleteWhenCorrect.eval(
                RScript.Mode.READ_WRITE,
                """
                        local val=redis.call('GET',KEYS[1])
                        if not val then
                            return 0
                        end
                        if val==ARGV[1] then
                            redis.call('DEL',KEYS[1])
                            return 1
                        else
                            return 2
                        end
                        """,
                RScript.ReturnType.VALUE,
                List.of(String.format(MAIL_VERIFY_KEY, mail)),
                code
        );
        if (result==0){
            return Optional.of(CODE_HAVE_EXPIRED);
        }
        return result==1? Optional.empty() : Optional.of(CODE_NOT_CORRECT);
    }

    //-1邮件没有发送过，0邮件发送过，可以重新发送，1邮件发送过，不能重新发送
    private int isMailHaveSent(String mail) {
        RBucket<String> sBucket = redissonClient.getBucket(
                String.format(MAIL_VERIFY_KEY, mail), StringCodec.INSTANCE
        );
        String s = sBucket.get();
        //没有这个键
        if (s == null) {
            return MAIL_NOT_SENT;
        }
        //有这个键，就检查时间是否超过了5分钟
        Duration time = Duration.ofSeconds(sBucket.getExpireTime());
        return MAIL_EXPIRE_TIME.compareTo(time.plus(MAIL_RESENT_TIME)) <= 0 ? MAIL_CANNOT_RESENT : MAIL_CAN_RESENT;
    }

    private void writeMailInRedis(String mail, String code) {
        RBucket<String> stringRBucket = redissonClient.getBucket(
                String.format(MAIL_VERIFY_KEY, mail), StringCodec.INSTANCE
        );
        stringRBucket.set(code, MAIL_EXPIRE_TIME);
    }

    private String getVerifyCode() {
        IntStream intStream = RandomGenerator.getDefault().ints(6, 0, 10);
        StringBuilder builder = new StringBuilder(6);
        intStream.forEach(builder::append);
        return builder.toString();
    }
}
