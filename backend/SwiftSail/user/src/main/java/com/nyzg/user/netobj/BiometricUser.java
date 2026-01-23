package com.nyzg.user.netobj;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BiometricUser {
    TokenUser tokenUser;
    String token;

    public BiometricUser(TokenUser tokenUser, String token) {
        this.tokenUser = tokenUser;
        this.token = token;
    }
}