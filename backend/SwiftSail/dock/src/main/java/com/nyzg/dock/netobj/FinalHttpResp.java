package com.nyzg.dock.netobj;

import com.nyzg.common.netobj.HttpResp;

public class FinalHttpResp {
    public final static HttpResp CLIENT_ID_EMPTY = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "clientId不能为空");
    public final static HttpResp CODE_VERIFIER_INVALID = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "非法code verifier");
    public final static HttpResp STATE_IS_NULL = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "您还没有授权，state为空");
    public final static HttpResp UNKNOWN_DEVICE = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "未知设备");
    public final static HttpResp LACK_USER_ID = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "缺少登录用户id");
    public final static HttpResp USER_ID_NOT_MATCH_TOKEN = new HttpResp(false, HttpResp.COMMON_ERROR_CODE, "请求中的userId和token中的userId不一致");
}
