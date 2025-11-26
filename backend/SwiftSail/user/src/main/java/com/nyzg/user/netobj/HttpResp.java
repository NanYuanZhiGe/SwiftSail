package com.nyzg.user.netobj;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class HttpResp {
    public static final int COMMON_SUCCESS_CODE=-1;
    public static final int COMMON_ERROR_CODE=0;
    public static final int SERVER_INTERNAL_ERROR=1;
    public static final int INVALID_USER_INPUT =2;
    boolean success;
    int code;
    String message;
    Object content;


    public HttpResp(boolean success, int code,String message) {
        this.success = success;
        this.code=code;
        this.message = message;
    }

    public HttpResp(boolean success, int code, String message, Object content) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.content = content;
    }
}
