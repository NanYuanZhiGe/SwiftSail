package com.nyzg.swiftsail.netobj;

public class RegisterUserReq {
    String nickName;
    String email;
    byte[] secretWord;

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
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