package com.nyzg.user.netobj;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MailVerifyReq {
    String mailAddr;

    public MailVerifyReq(String mailAddr) {
        this.mailAddr = mailAddr;
    }
}
