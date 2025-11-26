package com.nyzg.common.ss_utils;


import lombok.NonNull;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Random;
import java.util.stream.IntStream;

final public class EncryptThreadSafe {
    static private final String RANDOM_STRING_SOURCE = "abghyipjnbugfpjnuhfupknrgsxdgrgpnjneor234897jb3ryg94hthinbfyuvstfa7q9wuhfubgbcsuyg6yt89gh0iwenfjsbhzgtf1294u2-hojfbiaugcgvahskjdnfoe8r1937rgg";
    static private final ThreadLocal<MessageDigest> THREAD_LOCAL_MD_256 = new ThreadLocal<>();
    static private final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder();
    static private final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();
    static private final Base64.Decoder BASE64_DECODER = Base64.getDecoder();

    public static boolean verifyRsa2048Sign(String pubKeyBase64, String challenge, String signBase64) {
        byte[] pubKeyBytes = BASE64_DECODER.decode(pubKeyBase64);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pubKeyBytes);
        try {
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);
            byte[] signatureBytes = Base64.getDecoder().decode(signBase64);
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(challenge.getBytes(StandardCharsets.UTF_8));
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }

    public static String getBase64RandomString64() {
        IntStream intStream = new Random().ints(64, 0, RANDOM_STRING_SOURCE.length());
        StringBuilder builder = new StringBuilder(64);
        intStream.forEach(action -> {
            builder.append(RANDOM_STRING_SOURCE.charAt(action));
        });
        return builder.toString();
    }

    public static Tuple<String, String, String> getBase64UrlSha256RandomString() {
        //原始随机字符串，sha256过后的哈希字符串，base64编码的哈希字符串
        Tuple<String, String, String> result = new Tuple<>();
        //获取长度为64的随机字符串
        IntStream intStream = new Random().ints(64, 0, RANDOM_STRING_SOURCE.length());
        StringBuilder builder = new StringBuilder(64);
        intStream.forEach(action -> {
            builder.append(RANDOM_STRING_SOURCE.charAt(action));
        });
        result.setA(builder.toString());
        //对这个随机字符串取sha256的哈希值
        MessageDigest md_256 = getDigestThreadLocal();
        byte[] hashedByte = md_256.digest(result.getA().getBytes());
        //对这个byte[]数组按照base64格式进行编码
        String base64HashedString = BASE64_URL_ENCODER.encodeToString(hashedByte).replace("=", "");
        result.setB(new String(hashedByte));
        result.setC(base64HashedString);
        return result;
    }

    static public String getBase64UrlSha256WithCertainString(@NonNull String s) {
        MessageDigest md_256 = getDigestThreadLocal();
        byte[] hashedByte = md_256.digest(s.getBytes());
        return BASE64_URL_ENCODER.encodeToString(hashedByte).replace("=", "");
    }

    static public String transferStringToBase64EncodedString(@NonNull String s) {
        return BASE64_ENCODER.encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static MessageDigest getDigestThreadLocal() {
        MessageDigest md_256 = THREAD_LOCAL_MD_256.get();
        if (md_256 == null) {
            synchronized (EncryptThreadSafe.class) {
                md_256 = THREAD_LOCAL_MD_256.get();
                if (md_256 == null) {
                    try {
                        THREAD_LOCAL_MD_256.set(MessageDigest.getInstance("SHA-256"));
                        md_256 = THREAD_LOCAL_MD_256.get();
                    } catch (Exception e) {//不会发生
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return md_256;
    }
}
