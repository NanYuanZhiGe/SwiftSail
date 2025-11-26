package com.nyzg.user.netobj;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RegisterUserReq {
    String nickName;
    String email;
    byte[] secretWord;
}