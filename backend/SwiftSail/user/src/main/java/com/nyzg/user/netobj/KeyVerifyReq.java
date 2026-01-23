package com.nyzg.user.netobj;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class KeyVerifyReq {
    String email;
    String token;
    String deviceId;
}
