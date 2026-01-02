package com.nyzg.swiftsail.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.bean.GlobalConf;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.bean.GlobalToast;
import com.nyzg.swiftsail.bean.JsonSerializer;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.dao.LastLoginTable;
import com.nyzg.swiftsail.dao.UserTable;
import com.nyzg.swiftsail.dbobj.LastLogin;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.netobj.HttpResp;
import com.nyzg.swiftsail.netobj.Token;
import com.nyzg.swiftsail.repository.LoginRepository;

import java.util.List;
import java.util.Optional;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginWorker extends Worker {
    public LoginWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        //下面的这个函数隐含了更新用户的token
        Optional<User> lastLoginUser = checkLastLogin();
        //上一次的登录用户不存在，或者上一次的登录凭证到现在失效或者是无法验证登录凭证
        //就统一进入登录页面
        if (!lastLoginUser.isPresent()) {
            //这里需要查询用户的登录表，给登录界面用
            List<User> availableUserList = getAvailableUserList();
            //需要额外添加一个用户：“账号未列出？”
            User unListUser = new User();
            unListUser.setId(-1L);
            availableUserList.add(unListUser);
            LoginRepository.INSTANCE.getMutableAvailableUserList().postValue(availableUserList);
            return Result.failure();
        }
        //如果成功，保存当前的登录账户，更新当前的user就行了
        User successCheckUser = lastLoginUser.get();
        LoginRepository.INSTANCE.login(successCheckUser);
        return Result.success();
    }

    private List<User> getAvailableUserList() {
        SQLiteDB sqLiteDB = SQLiteDB.getDatabase(GlobalApplication.getAppContext());
        return sqLiteDB.userTable().selectAllUser();
    }

    private Optional<User> checkLastLogin() {
        SQLiteDB sqLiteDB = SQLiteDB.getDatabase(GlobalApplication.getAppContext());
        LastLoginTable lastLoginTable = sqLiteDB.lastLoginTable();
        List<LastLogin> lastLoginTableList = lastLoginTable.selectAllFromLastLoginTable();
        if (lastLoginTableList.isEmpty()) {//没有上一次的登录用户
            return Optional.empty();
        }
        LastLogin lastLogin = lastLoginTableList.get(0);
        if (lastLogin.userId == 0L) {//如果是本地账号登录
            return Optional.of(GlobalInstance.LOCAL_USER);
        }
        //用户账号登录
        //检查token是否有效，有效就更新token，然后返回该用户，无效返回空
        UserTable userTable = sqLiteDB.userTable();
        User lastLoginUser = userTable.selectSingleUser(lastLogin.userId);
        if (lastLoginUser == null) {//无效用户
            return Optional.empty();
        }
        String oldToken = userTable.selectTokenFromUserTable(lastLogin.userId);
        if (oldToken == null) {//无效token
            return Optional.empty();
        }
        //网路错误或者验签错误。需要重新登录
        Optional<String> newToken = acquireNewTokenSyncNullAtFail(oldToken);
        if (!newToken.isPresent()) {
            return Optional.empty();
        }
        //保存这个新token
        sqLiteDB.userTable().updateUserLoginToken(lastLogin.userId, newToken.get());
        return Optional.of(lastLoginUser);
    }

    private Optional<String> acquireNewTokenSyncNullAtFail(String oldToken) {
        OkHttpClient httpClient = GlobalInstance.okHttpClient;
        Request request = new Request.Builder().url(GlobalConf.URL_VERIFY_AND_GET_TOKEN).method(GlobalConf.POST, RequestBody.create(JsonSerializer.serialize(new Token(oldToken)), GlobalConf.APPLICATION_JSON)).build();
        HttpResp httpResp;
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() == null) {//没有响应数据
                return Optional.empty();
            }
            httpResp = JsonSerializer.deSerialize(response.body().string(), HttpResp.class);
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
