package com.nyzg.user.remoteobj;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RemoteUser {
    long id;
    String nickName;
    String email;
    String token;

    public RemoteUser(long id, String nickName, String email) {
        this.id = id;
        this.nickName = nickName;
        this.email = email;
    }
}