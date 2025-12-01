package com.nyzg.user.service;

import com.nyzg.common.ss_utils.EncryptThreadSafe;
import com.nyzg.user.dbobj.User;
import com.nyzg.user.mapper.UserTableMapper;
import com.nyzg.user.netobj.HttpResp;
import com.nyzg.user.netobj.RegisterUserReq;
import com.nyzg.user.remoteobj.RemoteUser;
import jakarta.annotation.Resource;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.rmi.Remote;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class DbService {
    private final HttpResp SERVER_CANNOT_WRITE_DB = new HttpResp(false, HttpResp.SERVER_INTERNAL_ERROR, "服务端无法写入数据");
    private final HttpResp NICK_NAME_SHOULD_UNIQUE = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "用户名已被其他用户使用");
    private final HttpResp EMAIL_NOT_EXISTS = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "用户不存在");
    private final HttpResp PASSWORD_NOT_CORRECT = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "密码不正确");
    private final HttpResp CANNOT_USE_SECRET_KEY = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "无法进行密钥验证");
    private final HttpResp CHALLENGE_NOT_FOUND = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "挑战吗未生成或挑战码已过期");
    private final HttpResp CHALLENGE_FAIL = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "挑战失败");
    private final String REDIS_CHALLENGE_KEY = "user.service.challenge.%s";
    private final Duration REDIS_CHALLENGE_DURATION = Duration.ofSeconds(10 * 60);
    @Resource
    RedissonClient redissonClient;
    @Resource
    IdService idService;
    @Resource
    UserTableMapper userTableMapper;

    public HttpResp insertUserSuccessReturnUser(RegisterUserReq req) {
        List<User> userList = userTableMapper.getUserByEmail(req.getEmail());
        if (!userList.isEmpty()) {//如果email存在，直接成功登录
            User user = userList.get(0);
            return new HttpResp(true, new RemoteUser(user.getId(), user.getNickName(), user.getEmail()));
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
        RemoteUser safeUser = new RemoteUser(user.getId(), user.getNickName(), user.getEmail());
        return new HttpResp(true, safeUser);
    }

    public Optional<HttpResp> onNullWhenPasswordCorrect(String email, byte[] secretWord) {
        try {
            int rows = userTableMapper.isEmailExists(email);
            if (rows == 0) {
                return Optional.of(EMAIL_NOT_EXISTS);
            }
            rows = userTableMapper.isHaveEmailAndSecretWord(email, secretWord);
            if (rows == 0) {
                return Optional.of(PASSWORD_NOT_CORRECT);
            }
        } catch (Exception e) {
            return Optional.of(SERVER_CANNOT_WRITE_DB);
        }
        return Optional.empty();
    }

    public Optional<HttpResp> onNullWhenSecretChallengeSucceed(String email, String sign) {
        RBucket<String> stringRBucket = redissonClient.getBucket(String.format(REDIS_CHALLENGE_KEY, email), StringCodec.INSTANCE);
        String challenge = stringRBucket.get();
        if (challenge == null) {
            return Optional.of(CHALLENGE_NOT_FOUND);
        }
        try {
            List<User> users = userTableMapper.getSecretPubKey(email);
            if (users.isEmpty() || users.get(0).getSecretPubKey() == null) {
                return Optional.of(CANNOT_USE_SECRET_KEY);
            }
            if (!EncryptThreadSafe.verifyRsa2048Sign(users.get(0).getSecretPubKey(), challenge, sign)) {
                return Optional.of(CHALLENGE_FAIL);
            }
        } catch (Exception e) {
            return Optional.of(SERVER_CANNOT_WRITE_DB);
        }
        return Optional.empty();
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

    public HttpResp getSecretChallenge(String mail) {
        if (userTableMapper.isHaveEmailAndSecretKey(mail) == 0) {
            return CANNOT_USE_SECRET_KEY;
        }
        String challenge = EncryptThreadSafe.getBase64RandomString64();
        RBucket<String> stringRBucket = redissonClient.getBucket(String.format(REDIS_CHALLENGE_KEY, mail), StringCodec.INSTANCE);
        stringRBucket.set(challenge, REDIS_CHALLENGE_DURATION);
        return new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "", challenge);
    }
}
