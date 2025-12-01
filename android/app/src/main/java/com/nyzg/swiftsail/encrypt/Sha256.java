package com.nyzg.swiftsail.encrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Sha256 {
    public static byte[] generateSha256ByteArray(String str) {
        if (str == null) {
            return null;
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("sha-256");
            return messageDigest.digest(str.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {//never happen
            return null;
        }
    }
}
