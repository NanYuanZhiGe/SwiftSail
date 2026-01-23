package com.nyzg.user.service;

import cn.hutool.core.date.DateUtil;
import com.nyzg.common.TimeUtils;
import com.nyzg.common.ss_utils.JwtThreadSafe;
import com.nyzg.user.conf.JWTConf;
import com.nyzg.user.dbobj.User;
import com.nyzg.user.mapper.UserTableMapper;
import com.nyzg.user.netobj.TokenUser;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TokenService {
    @Resource
    JWTConf jwtConf;
    @Resource
    UserTableMapper userTableMapper;

    public Optional<TokenUser> getJwtToken(TokenUser tokenUser) {
        if (tokenUser == null || tokenUser.getEmail() == null || tokenUser.getId() == 0L) {
            return Optional.empty();
        }
        tokenUser.setToken(JwtThreadSafe.getJwtTokenHMac(
                Map.of("email", tokenUser.getEmail(), "userId", tokenUser.getId()),
                DateUtil.date(),
                DateUtil.date(),
                24,
                jwtConf.getHsKey()
        ));
        return Optional.of(tokenUser);
    }

    public Optional<TokenUser> getJwtToken(String mail, long userId) {
        List<User> userList = userTableMapper.getUserByEmail(mail);
        if (userList.isEmpty()) {
            return Optional.empty();
        }
        User user = userList.get(0);
        long createTime= TimeUtils.toEpochDays(user.getCreateTime());
        String token=JwtThreadSafe.getJwtTokenHMac(
                Map.of("email", mail, "userId", userId),
                DateUtil.date(),
                DateUtil.date(),
                24,
                jwtConf.getHsKey()
        );
        TokenUser tokenUser = new TokenUser(
                user.getId(),user.getNickName(),user.getEmail(),token,createTime
        );
        return Optional.of(tokenUser);
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
