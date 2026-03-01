package com.nyzg.swiftsail.repository;

import androidx.lifecycle.MutableLiveData;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.bean.MyJsonSerializer;
import com.nyzg.swiftsail.bean.NetWorkBuilder;
import com.nyzg.swiftsail.bean.NetWorkHandler;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.bean.ServerURL;
import com.nyzg.swiftsail.dao.LastLoginTable;
import com.nyzg.swiftsail.dao.UserTable;
import com.nyzg.swiftsail.dbobj.LastLogin;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.netobj.Token;
import com.nyzg.swiftsail.obj.Pair;
import com.nyzg.swiftsail.obj.SucceedOrNot;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import okhttp3.Request;
import okhttp3.RequestBody;

public class LoginRepository {
    private volatile static LoginRepository INSTANCE = null;

    public static LoginRepository getInstance() {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        synchronized (LoginRepository.class) {
            if (INSTANCE != null) {
                return INSTANCE;
            }
            INSTANCE = new LoginRepository();
        }
        return INSTANCE;
    }

    public final MutableLiveData<User> currentUser = new MutableLiveData<>();
    public final MutableLiveData<User> onLoginUser = new MutableLiveData<>();
    public final MutableLiveData<List<User>> availableUserList = new MutableLiveData<>();
    private volatile boolean onLastLogin = false;

    private LoginRepository() {
    }

    public void setCurrentUser(User user) {
        currentUser.setValue(user);
    }

    public MutableLiveData<User> getCurrentUser() {
        return currentUser;
    }

    /**
     * 以本地用户需要做四件事:
     * 1. 更新上一次的登录账户
     * 2. 更新当前用户
     * 3. 从云端同步用户的运动记录
     * 4. 更新用户的运动记录
     */
    public void login(User user) {
        //第一件事，更新上一次的登录账户，异步执行，避免阻塞
        CompletableFuture.supplyAsync(() -> {
            SQLiteDB sqLiteDB = SQLiteDB.getDatabase(GlobalApplication.getAppContext());
            LastLoginTable lastLoginTable = sqLiteDB.lastLoginTable();
            LastLogin lastLogin = new LastLogin();
            lastLogin.userId = user.id;
            lastLoginTable.deleteAll();
            lastLoginTable.insertLastLogin(lastLogin);
            return null;
        });
        //第二件事，更新当前用户
        LoginRepository.INSTANCE.getCurrentUser().postValue(user);
        //第三件事，从云端用户的运动记录
        if (user.id == 0L) {//如果是本地账户的话就不需要
            //第四件事，更新用户的运动记录
            RecordRepository.getInstance().initUserRecordDataAsync(GlobalInstance.LOCAL_USER);
            return;
        }
        CompletableFuture.supplyAsync(() -> {

            return null;
        }).thenAccept(
                //回调里面更新用户的运动记录
                action -> RecordRepository.getInstance().initUserRecordDataAsync(user)
        );
    }

    /**
     * 这个函数只会也只能执行一次，也就是用户第一次进入到这个应用里面的时候
     * 从数据库中读取上一次的登录用户，尝试使用这个用户进行登录
     * 如果上次登录用户为空，token过期，无法连接服务器，返回FAIL，
     * 如果拿到了token，返回SUCCESS，里面会post更新currentUser
     */
    public CompletableFuture<Pair<SucceedOrNot, String>> tryLastLoginAsync(Consumer<String> onFail) {
        return CompletableFuture.supplyAsync(() -> {
            if (onLastLogin) {
                return new Pair<>(SucceedOrNot.FAIL, "正在登录");
            }
            onLastLogin = true;
            //下面的这个函数隐含了更新用户的token
            Optional<User> lastLoginUser = checkLastLogin(onFail);
            //上一次的登录用户不存在，或者上一次的登录凭证到现在失效或者是无法验证登录凭证
            //就统一进入登录页面
            if (!lastLoginUser.isPresent()) {
                //这里需要查询用户的登录表，给登录界面用
                List<User> availableUserList = getAvailableUserList();
                //需要额外添加一个用户：“账号未列出？”
                User unListUser = new User();
                unListUser.id = -1L;
                availableUserList.add(unListUser);
                LoginRepository.INSTANCE.availableUserList.postValue(availableUserList);
                return new Pair<>(SucceedOrNot.FAIL, "登录失败");
            }
            //如果成功，保存当前的登录账户，更新当前的user就行了
            User successCheckUser = lastLoginUser.get();
            LoginRepository.INSTANCE.login(successCheckUser);
            onLastLogin = false;
            return new Pair<>(SucceedOrNot.SUCCEED, "登录成功");
        });
    }

    /**
     * 用于辅助tryLastLoginAsync
     * 获取可用的用户列表
     */
    private List<User> getAvailableUserList() {
        SQLiteDB sqLiteDB = SQLiteDB.getDatabase(GlobalApplication.getAppContext());
        return sqLiteDB.userTable().selectAllUser();
    }

    /**
     * 用于辅助tryLastLoginAsync
     * 检查上一次的登录是否有效
     */
    private Optional<User> checkLastLogin(Consumer<String> onFail) {
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
        Optional<String> newToken = acquireNewTokenSyncNullAtFail(oldToken, onFail);
        if (!newToken.isPresent()) {
            return Optional.empty();
        }
        //保存这个新token
        sqLiteDB.userTable().updateUserLoginToken(lastLogin.userId, newToken.get());
        return Optional.of(lastLoginUser);
    }

    /**
     * 用于辅助tryLastLoginAsync
     * 根据上一次登录用户的token，获取新的token
     */
    private Optional<String> acquireNewTokenSyncNullAtFail(String oldToken, Consumer<String> onFail) {
        AtomicReference<Optional<String>> result = new AtomicReference<>(Optional.empty());
        NetWorkHandler.handleNetRespBeforeLogin(
                NetWorkBuilder.doChunkRequest(
                        new Request.Builder()
                                .url(ServerURL.URL_VERIFY_AND_GET_TOKEN)
                                .method(
                                        ServerURL.POST,
                                        RequestBody.create(MyJsonSerializer.serialize(new Token(oldToken)), ServerURL.APPLICATION_JSON))
                                .build()
                ),
                onFail,
                token -> {
                    if (token == null || token.getToken() == null || token.getToken().isEmpty()) {
                        result.set(Optional.empty());
                        return;
                    }
                    result.set(Optional.of(token.getToken()));
                },
                Token.class
        );
        return result.get();
    }

    /**
     * 这个函数有幂等性，放心使用，如果你是登录这个会直接更新token
     * 就不要再调用updateUserTokenAsync了
     * 请保证你的输入的user是完整的，尤其时token和id
     */
    public void updateUserToLocalAccountAsync(User user) {
        CompletableFuture.supplyAsync(() -> {
            if (user.id == 0L) {//本地账户
                return null;
            }
            //如果这个用户是已经存在过的，就不要再插入了
            SQLiteDB sqLiteDB = SQLiteDB.getDatabase(GlobalApplication.getAppContext());
            Long userId = sqLiteDB.userTable().selectUserIdByEmail(user.email);
            if (userId == null) {//不存在就插入
                sqLiteDB.userTable().insertUser(user);
            } else {//如果存在就刷新token
                sqLiteDB.userTable().updateUserLoginToken(user.id, user.token);
            }
            return null;
        });
    }
}