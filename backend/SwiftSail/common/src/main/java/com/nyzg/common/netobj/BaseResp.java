package com.nyzg.common.netobj;

public class BaseResp {
    public static final HttpResp LOGIN_REQUIRED=new HttpResp(false,"需要登录才能使用此功能");
    public static final HttpResp INTERNAL_ERROR=new HttpResp(false,"内部处理逻辑错误，非IO");
}
