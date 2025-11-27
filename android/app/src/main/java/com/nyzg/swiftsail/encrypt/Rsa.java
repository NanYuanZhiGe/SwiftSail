package com.nyzg.swiftsail.encrypt;

import com.nyzg.swiftsail.obj.Pair;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Optional;

public class Rsa {
    public static Optional<Pair<String, String>> notNullGenerateRsa2048KeyPairSucceed() {
        KeyPairGenerator keyGen = null;
        try {
            keyGen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            return Optional.empty();
        }
        keyGen.initialize(2048); // 指定 2048 位
        KeyPair keyPair = keyGen.generateKeyPair();

        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        String base64PublicKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        String base64PrivateKey = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        return Optional.of(new Pair<>(base64PublicKey, base64PrivateKey));
    }

}
