package com.nyzg.swiftsail.netobj;

public class MailVerifyReq {
    String mail;


    public MailVerifyReq(){}
    public MailVerifyReq(String mail) {
        this.mail = mail;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }
}
