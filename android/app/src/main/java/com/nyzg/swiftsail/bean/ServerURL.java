package com.nyzg.swiftsail.bean;

import java.net.URI;
import java.net.URL;

import okhttp3.MediaType;

public class ServerURL {
    final public static String BASE_URL = "https://sport.nyzghencute.top";
    final public static String BASE_WEBSOCKET = "wss://sport.nyzghencute.top";
    final public static String URL_USER_PREFIX = "/api/user";
    final public static String URL_DOCK_PREFIX = "/api/dock";

    //login
    final public static String URL_REGISTER_VERIFY_CODE = BASE_URL + URL_USER_PREFIX + "/login/register/user";
    final public static String URL_REGISTER_SUBMIT = BASE_URL + URL_USER_PREFIX + "/login/register/verify/mail/code";
    final public static String URL_LOGIN_VERIFY_CODE = BASE_URL + URL_USER_PREFIX + "/login/get/mail/verify/code";
    final public static String URL_LOGIN_WITH_MAIL = BASE_URL + URL_USER_PREFIX + "/login/verify/mail/code";
    final public static String URL_LOGIN_WITH_PASSWORD = BASE_URL + URL_USER_PREFIX + "/login/verify/password";
    final public static String URL_VERIFY_AND_GET_TOKEN = BASE_URL + URL_USER_PREFIX + "/login/verify/get/token";
    final public static String URL_LOGIN_WITH_KEY = BASE_URL + URL_USER_PREFIX + "/login/verify/secret/key";
    final public static String URL_REGISTER_BIOMETRIC = BASE_URL + URL_USER_PREFIX + "/login/biometric/add";
    //dock
    final public static URL URL_SYNC_BACKUP_RECORD;
    final public static URL URL_IS_WATCH_ADDED;
    final public static URL URL_SYNC_STOP;
    final public static URL URL_CHECK_SYNC_STATUS;
    final public static URL URL_ACQUIRE_SYNC_DATA;
    final public static URL URL_GET_WATCH_LIST;
    final public static URL URL_WATCH_DELETE;
    final public static URL URL_GO_OFFLINE;
    final public static URL URL_GET_DAY_DATA;
    final public static URI URI_WEBSOCKET_REPORT_SYNC;

    static {
        try {
            URL_SYNC_BACKUP_RECORD = new URL(BASE_URL + URL_DOCK_PREFIX + "/sync/record/backup");
            URL_IS_WATCH_ADDED = new URL(BASE_URL + URL_DOCK_PREFIX + "/is/watch/added");
            URL_SYNC_STOP = new URL(BASE_URL + URL_DOCK_PREFIX + "/sync/stop");
            URL_CHECK_SYNC_STATUS = new URL(BASE_URL + URL_DOCK_PREFIX + "/check/sync/status");
            URL_ACQUIRE_SYNC_DATA = new URL(BASE_URL + URL_DOCK_PREFIX + "/acquire/sync/data");
            URL_GET_WATCH_LIST = new URL(BASE_URL + URL_DOCK_PREFIX + "/get/watchList");
            URL_WATCH_DELETE = new URL(BASE_URL + URL_DOCK_PREFIX + "/watch/delete");
            URL_GO_OFFLINE = new URL(BASE_URL + URL_DOCK_PREFIX + "/go/offline");
            URL_GET_DAY_DATA = new URL(BASE_URL + URL_DOCK_PREFIX + "/get/data/day");
            URI_WEBSOCKET_REPORT_SYNC = new URI(BASE_WEBSOCKET + URL_DOCK_PREFIX + "/ws/dock/websocket/report/sync");
        } catch (Exception e) {//正常情况下是不会发生的
            throw new RuntimeException(e);
        }
    }

    final public static String URL_TILE_SERVER = "https://tile.nyzghencute.top/styles/basic-preview/style.json";
    final public static String POST = "POST";
    final public static String GET = "GET";
    final public static MediaType APPLICATION_JSON = MediaType.get("application/json; charset=utf-8");
}