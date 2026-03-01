package com.nyzg.common.netobj;

public class BaseResp {
    public static final HttpResp LOGIN_REQUIRED=new HttpResp(false,"需要登录才能使用此功能");
    public static final HttpResp INTERNAL_ERROR=new HttpResp(false,"内部处理逻辑错误，非IO");
    public final static HttpResp LARGE_IMAGE_IS_NOT_ALLOW = new HttpResp(false, "图片太大，不允许上传");
    public final static HttpResp SERVER_TOO_BUSY=new HttpResp(false,"服务器繁忙，拒绝处理请求");
}
