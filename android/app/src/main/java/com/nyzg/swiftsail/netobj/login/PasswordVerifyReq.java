package com.nyzg.swiftsail.netobj.login;


public class PasswordVerifyReq {
    String email;
    byte[] secretWord;
    public PasswordVerifyReq(){}

    public PasswordVerifyReq(String email, byte[] secretWord) {
        this.email = email;
        this.secretWord = secretWord;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public byte[] getSecretWord() {
        return secretWord;
    }

    public void setSecretWord(byte[] secretWord) {
        this.secretWord = secretWord;
    }
}