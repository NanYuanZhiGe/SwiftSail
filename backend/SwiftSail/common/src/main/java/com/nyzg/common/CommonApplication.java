package com.nyzg.common;

import com.nyzg.common.ss_utils.EncryptThreadSafe;

public class CommonApplication {
    public static void main(String[] args){
        System.out.println(EncryptThreadSafe.getBase64UrlSha256WithCertainString("01234567890123456789012345678901234567890123456789"));
    }
}
