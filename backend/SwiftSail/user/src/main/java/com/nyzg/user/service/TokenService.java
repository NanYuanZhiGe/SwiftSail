package com.nyzg.user.service;

import com.nyzg.common.ss_utils.EncryptThreadSafe;
import com.nyzg.user.mapper.UserTableMapper;
import com.nyzg.user.netobj.HttpResp;
import jakarta.annotation.Resource;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TokenService {

    public HttpResp getJwtTokenResp(String mail) {

        return new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "");
    }
}
