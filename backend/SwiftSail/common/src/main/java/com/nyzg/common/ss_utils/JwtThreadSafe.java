package com.nyzg.common.ss_utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.nyzg.common.Pair;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtThreadSafe {
    public static enum Status {
        EXPIRE, FAIL, SUCCESS;
    }

    public static String getJwtTokenHMac(Map<String, Object> payload, Date iat, Date nbf, int expHour, byte[] hsKey) {
        Map<String, Object> tokenMap = new HashMap<>(payload);
        tokenMap.put("iss", "SwiftSail");
        tokenMap.put("sub", "login");
        tokenMap.put("iat", iat);
        tokenMap.put("nbf", nbf);
        tokenMap.put("exp", DateUtil.offsetHour(nbf, expHour));
        JWTSigner signer = JWTSignerUtil.hs512(hsKey);
        return JWTUtil.createToken(tokenMap, signer);
    }

    public static Pair<Status, Map<String, Object>> validateJwtTokenHMac(String token, byte[] hsKey) {
        JWT jwt = JWTUtil.parseToken(token);
        JWTSigner signer = JWTSignerUtil.hs512(hsKey);

        jwt.setSigner(signer);
        if (!jwt.verify()) {
            return new Pair<>(Status.FAIL, null);
        }
        try {
            JWTValidator.of(jwt).validateDate(DateUtil.date(), 0);
            return new Pair<>(Status.SUCCESS, jwt.getPayloads());
        } catch (Exception e) {
            return new Pair<>(Status.EXPIRE, null);
        }
    }
}
