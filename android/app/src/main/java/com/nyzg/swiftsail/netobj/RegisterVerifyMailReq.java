package com.nyzg.swiftsail.netobj;


public class RegisterVerifyMailReq extends RegisterUserReq{
    String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
