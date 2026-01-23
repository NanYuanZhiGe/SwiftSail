package com.nyzg.common.netobj;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class HttpResp {
    public static final int COMMON_SUCCESS_CODE = -1;
    public static final int COMMON_ERROR_CODE = 0;
    public static final int SERVER_INTERNAL_ERROR = 1;
    public static final int INVALID_USER_INPUT = 2;
    public static final HttpResp COMMON_SUCCESS = new HttpResp(true, COMMON_SUCCESS_CODE, "");
    public static final HttpResp COMMON_ERROR = new HttpResp(false, COMMON_ERROR_CODE, "");
    boolean success;
    int code;
    String message;
    Object content;


    public HttpResp(boolean success, Object content) {
        this.success = success;
        if (success) {
            code = COMMON_SUCCESS_CODE;
        } else {
            code = COMMON_ERROR_CODE;
        }
        message = "";
        this.content = content;
    }

    public HttpResp(boolean success, int code, String message) {
        this.success = success;
        this.code = code;
        this.message = message;
    }

    public HttpResp(boolean success, int code, String message, Object content) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.content = content;
    }
}
