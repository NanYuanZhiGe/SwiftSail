package com.nyzg.swiftsail.netobj;

import java.util.Map;

public class HttpResp{
    boolean success;
    int code;
    String message;
    Map<String,Object> content;

    public boolean isFail() {
        return !success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getContent() {
        return content;
    }

    public void setContent(Map<String, Object> content) {
        this.content = content;
    }
}
