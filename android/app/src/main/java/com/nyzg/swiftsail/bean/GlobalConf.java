package com.nyzg.swiftsail.bean;

import okhttp3.MediaType;

public class GlobalConf {
    final public static String BASE_HOST = "http://192.168.48.42:8080";
    final public static String URL_REGISTER_VERIFY_CODE = BASE_HOST + "/register/user";
    final public static String URL_REGISTER_SUBMIT = BASE_HOST + "/register/verify/mail/code";
    final public static String URL_LOGIN_VERIFY_CODE = BASE_HOST + "/login/get/mail/verify/code";
    final public static String URL_LOGIN_WITH_MAIL = BASE_HOST + "/login/verify/mail/code";
    final public static String URL_LOGIN_WITH_PASSWORD = BASE_HOST + "/login/verify/password";
    final public static String URL_VERIFY_AND_GET_TOKEN=BASE_HOST+"/login/verify/get/token";
    final public static String URL_LOGIN_CHALLENGE = BASE_HOST + "/login/get/secret/challenge";
    final public static String URL_LOGIN_WITH_KEY = BASE_HOST + "/login/verify/secret/key";
    final public static String POST = "POST";
    final public static String GET = "GET";

    final public static MediaType APPLICATION_JSON = MediaType.get("application/json; charset=utf-8");
}