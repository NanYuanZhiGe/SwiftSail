package com.nyzg.user.dbobj;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class User {
    long id;
    String nickName;
    String email;
    byte[] secretWord;
    short userCharacter=0;
    String secretPubKey;
    LocalDateTime createTime;
    LocalDateTime modifyTime;

    public User(long id, String nickName, String email, byte[] secretWord) {
        this.id = id;
        this.nickName = nickName;
        this.email = email;
        this.secretWord = secretWord;
    }
}
