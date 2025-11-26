package com.nyzg.user.service;

import cn.hutool.core.net.NetUtil;
import jakarta.annotation.Resource;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import cn.hutool.core.lang.Snowflake;

@Service
public class IdService {
    @Resource
    RedissonClient redissonClient;
    private final Snowflake snowflake = new Snowflake(NetUtil.ipv4ToLong(NetUtil.getLocalhostStr()) & 31L);

    public long getId() {
        return snowflake.nextId();
    }
}