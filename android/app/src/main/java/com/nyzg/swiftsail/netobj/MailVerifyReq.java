package com.nyzg.swiftsail.netobj;

public class MailVerifyReq {
    String mailAddr;


    public MailVerifyReq() {
    }

    public MailVerifyReq(String mailAddr) {
        this.mailAddr = mailAddr;
    }

    public String getMailAddr() {
        return mailAddr;
    }

    public void setMailAddr(String mailAddr) {
        this.mailAddr = mailAddr;
    }
}
