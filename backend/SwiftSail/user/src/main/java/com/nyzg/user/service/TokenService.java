package com.nyzg.user.service;

import cn.hutool.core.date.DateUtil;
import com.nyzg.common.ss_utils.EncryptThreadSafe;
import com.nyzg.common.ss_utils.JwtThreadSafe;
import com.nyzg.user.conf.JWTConf;
import com.nyzg.user.mapper.UserTableMapper;
import com.nyzg.user.netobj.HttpResp;
import com.nyzg.user.netobj.Token;
import jakarta.annotation.Resource;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Service
public class TokenService {
    @Resource
    JWTConf jwtConf;

    public Token getJwtToken(String mail, long userId) {
        return new Token(JwtThreadSafe.getJwtTokenHMac(
                Map.of("email", mail, "userId", userId),
                DateUtil.date(),
                DateUtil.date(),
                24,
                jwtConf.getHsKey()
        ));
    }

    public Optional<String> getJwtTokenFromLastNullAtFail(Map<String, Object> payload) {
        try {
            return Optional.of(JwtThreadSafe.getJwtTokenHMac(
                    Map.of("email", payload.get("email"), "userId", payload.get("userId")),
                    DateUtil.date(),
                    DateUtil.date(),
                    24,
                    jwtConf.getHsKey()
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
