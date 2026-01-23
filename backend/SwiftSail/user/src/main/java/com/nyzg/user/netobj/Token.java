package com.nyzg.user.netobj;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Token {
    String token;

    public Token(String token) {
        this.token = token;
    }
}
