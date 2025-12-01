package com.nyzg.swiftsail.bean;

import android.os.Message;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.MainActivity;
import com.nyzg.swiftsail.dbobj.LastLogin;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.netobj.HttpResp;
import com.nyzg.swiftsail.netobj.Token;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GlobalFunction {
    public static void handleNetResp(Response response, Runnable onFail, Consumer<HttpResp> onSuccess) {
        if (response == null) {
            GlobalToast.SERVER_NOT_RESPONSE.run();
            onFail.run();
            return;
        }
        if (!response.isSuccessful()) {
            GlobalToast.RESPONSE_ERROR.accept(response.code());
            onFail.run();
            response.close();
            return;
        }
        try {
            HttpResp httpResp = JsonSerializer.deSerialize(response.body() != null ? response.body().string() : null, HttpResp.class);
            if (httpResp == null || !httpResp.isSuccess()) {
                GlobalToast.RESPONSE_NOT_SUCCESS.accept(httpResp == null ? null : httpResp.getMessage());
                onFail.run();
                response.close();
                return;
            }
            onSuccess.accept(httpResp);
        } catch (Exception e) {
            GlobalToast.CONTENT_UNACCEPTABLE.run();
        } finally {
            onFail.run();
            response.close();
        }
    }

    /**
     * 用户的注册成功是有两种逻辑的
     * 一种是用户没有注册过，所以要插入这个数据表
     * 一种是用户注册过了（以邮箱为准），就不要动了，更新token就好
     */
    public static void safeInsertRegisterUserSync(User user) {
        SQLiteDB sqLiteDB = SQLiteDB.getDatabase(GlobalApplication.getAppContext());
        Long userId = sqLiteDB.userTable().selectUserIdByEmail(user.getEmail());
        if (userId == null) {
            sqLiteDB.userTable().insertUser(user);
        }
    }

    public static void onSuccessLoginSync(String email, Map<String, Object> respContent) {
        if (respContent == null) {
            GlobalToast.SERVER_RESP_UNACCEPTABLE.run();
            return;
        }
        String token;
        try {
            token = (String) respContent.get("token");
        } catch (Exception e) {
            GlobalToast.SERVER_RESP_UNACCEPTABLE.run();
            return;
        }
        onSuccessLoginSync(email, token);
    }

    public static void onSuccessLoginSync(String email, String token) {
        SQLiteDB sqLiteDB = SQLiteDB.getDatabase(GlobalApplication.getAppContext());
        Long userId = sqLiteDB.userTable().selectUserIdByEmail(email);
        if (userId == null) {
            GlobalToast.COMMON_TOAST.accept("用户id不存在");
            return;
        }
        onSuccessLoginSync(userId, token);
    }

    public static void onSuccessLoginSync(long userId, String token) {
        SQLiteDB sqLiteDB = SQLiteDB.getDatabase(GlobalApplication.getAppContext());
        User user = sqLiteDB.userTable().selectSingleUser(userId);
        if (user == null) {
            GlobalToast.COMMON_TOAST.accept(String.format("用户不存在，用户id无效：%s", userId));
            return;
        }
        //保存Token至数据库
        sqLiteDB.userTable().updateUserLoginToken(userId, token);
        //更新磁盘中的最近的登录的用户表
        sqLiteDB.lastLoginTable().deleteAll();
        sqLiteDB.lastLoginTable().insertLastLogin(new LastLogin(userId));
        //更新内存中的当前登录用户数据
        user.setToken(token);
        GlobalInstance.currentUser.set(user);
        //返回主界面
        goBackToMainPage();
    }

    public static void goBackToMainPage() {
        Message message = Message.obtain();
        message.what = MainActivity.GO_BACK_TO_MAIN_PAGE;
        MainActivity.getHandler().sendMessage(message);
    }

    public static Optional<String> acquireNewTokenSyncNullAtFail(String oldToken) {
        OkHttpClient httpClient = GlobalInstance.okHttpClient;
        Request request = new Request.Builder()
                .url(GlobalConf.URL_VERIFY_AND_GET_TOKEN)
                .method(GlobalConf.POST,
                        RequestBody.create(JsonSerializer.serialize(new Token(oldToken)), GlobalConf.APPLICATION_JSON)
                )
                .build();
        HttpResp httpResp;
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() == null) {//没有响应数据
                return Optional.empty();
            }
            httpResp = JsonSerializer.deSerialize(
                    response.body().string(),
                    HttpResp.class
            );
            if (!httpResp.isSuccess()) {//验证失败
                GlobalToast.COMMON_TOAST.accept(httpResp.getMessage());
                return Optional.empty();
            }
            //返回的就是新的token
            Token token = JsonSerializer.mapToObject(httpResp.getContent(), Token.class).orElse(null);
            if (token == null || token.getToken() == null || token.getToken().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(token.getToken());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
