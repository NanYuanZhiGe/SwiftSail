package com.nyzg.swiftsail.netobj.login;

public class MailCodeVerifyReq {
    String mail;
    String code;


    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public MailCodeVerifyReq() {
    }

    public MailCodeVerifyReq(String mail, String code) {
        this.mail = mail;
        this.code = code;
    }
}
