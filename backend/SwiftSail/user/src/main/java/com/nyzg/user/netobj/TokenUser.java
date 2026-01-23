package com.nyzg.user.netobj;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TokenUser {
    long id;
    String nickName;
    String email;
    String token;
    long createTime;

    public TokenUser(long id, String nickName, String email, String token, long createTime) {
        this.id = id;
        this.nickName = nickName;
        this.email = email;
        this.token = token;
        this.createTime = createTime;
    }

    public TokenUser(long id, String nickName, String email, long createTime) {
        this.id = id;
        this.nickName = nickName;
        this.email = email;
        this.createTime = createTime;
    }
}
