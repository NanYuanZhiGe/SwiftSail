package com.nyzg.user.netobj;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MailCodeVerifyReq {
    String mail;
    String code;

    public MailCodeVerifyReq(String mail, String code) {
        this.mail = mail;
        this.code = code;
    }
}
