package com.nyzg.swiftsail.bean;

public class GlobalConf {
    public static String BASE_HOST = "http://127.0.0.1:8080";
    public static String URL_REGISTER_VERIFY_CODE = BASE_HOST + "/register/user";
    public static String URL_REGISTER_SUBMIT = BASE_HOST + "/register/verify/mail/code";
    public static String URL_LOGIN_VERIFY_CODE = BASE_HOST + "/login/get/mail/verify/code";
    public static String URL_LOGIN_WITH_MAIL = BASE_HOST + "/login/verify/mail/code";
    public static String URL_LOGIN_WITH_PASSWORD = BASE_HOST + "/login/verify/password";
    public static String URL_LOGIN_CHALLENGE = BASE_HOST + "/login/get/secret/challenge";
    public static String URL_LOGIN_WITH_KEY = BASE_HOST + "/login/verify/secret/key";

}
