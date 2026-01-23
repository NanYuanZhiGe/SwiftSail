package com.nyzg.user.netobj;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BiometricAddReq {
    String email;
    String deviceName;
    String deviceId;
    byte[] secretWord;
}