package com.nyzg.swiftsail.repository;

import androidx.lifecycle.MutableLiveData;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.bean.GlobalToast;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.dao.LastLoginTable;
import com.nyzg.swiftsail.dbobj.LastLogin;
import com.nyzg.swiftsail.dbobj.User;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LoginRepository {
    public final static LoginRepository INSTANCE = new LoginRepository();
    private final MutableLiveData<User> mutableCurrentUser = new MutableLiveData<>();
    private final MutableLiveData<List<User>> mutableAvailableUserList = new MutableLiveData<>();

    private LoginRepository() {
    }

    public void setCurrentUser(User user) {
        mutableCurrentUser.setValue(user);
    }

    public void setAvailableUserList(List<User> userList) {
        this.mutableAvailableUserList.setValue(userList);
    }

    public MutableLiveData<User> getMutableCurrentUser() {
        return mutableCurrentUser;
    }

    public MutableLiveData<List<User>> getMutableAvailableUserList() {
        return mutableAvailableUserList;
    }

    /**
     * 这个函数有幂等性，放心使用，如果你是登录这个会直接更新token
     * 就不要再调用updateUserTokenAsync了
     */
    public void updateUserToLocalAccountAsync(User user) {
        CompletableFuture.supplyAsync(() -> {
            if (user.getId()==0L){//本地账户
                return null;
            }
            //如果这个用户是已经存在过的，就不要再插入了
            SQLiteDB sqLiteDB = SQLiteDB.getDatabase(GlobalApplication.getAppContext());
            Long userId = sqLiteDB.userTable().selectUserIdByEmail(user.getEmail());
            if (userId == null) {
                sqLiteDB.userTable().insertUser(user);
            }
            return null;
        });
    }

    public void updateUserTokenAsync(long userId, String token) {
        SQLiteDB sqLiteDB = SQLiteDB.getDatabase(GlobalApplication.getAppContext());
        User user;
        if (userId == 0L) {//如果是本地用户，不需要保存
            return;
        } else {
            //检查这个用户是否真的存在于用户列表中
            user = sqLiteDB.userTable().selectSingleUser(userId);
            if (user == null) {
                GlobalToast.COMMON_TOAST.accept(String.format("用户不存在：%s", user.getNickName()));
                return;
            }
            //保存Token至数据库
            sqLiteDB.userTable().updateUserLoginToken(userId, token);
            user.setToken(token);
        }
    }

    /*
    以本地用户需要做四件事:
    1. 更新上一次的登录账户
    2. 更新当前用户
    3. 从云端同步用户的运动记录
    4. 更新用户的运动记录
     */
    public void login(User user) {
        //第一件事，更新上一次的登录账户，异步执行，避免阻塞
        CompletableFuture.supplyAsync(() -> {
            SQLiteDB sqLiteDB = SQLiteDB.getDatabase(GlobalApplication.getAppContext());
            LastLoginTable lastLoginTable = sqLiteDB.lastLoginTable();
            LastLogin lastLogin = new LastLogin();
            lastLogin.userId = user.getId();
            lastLoginTable.deleteAll();
            lastLoginTable.insertLastLogin(lastLogin);
            return null;
        });
        //第二件事，更新当前用户
        LoginRepository.INSTANCE.getMutableCurrentUser().postValue(user);
        //第三件事，从云端用户的运动记录
        if (user.getId() == 0L) {//如果是本地账户的话就不需要
            //第四件事，更新用户的运动记录
            RecordRepository.INSTANCE.refreshRecordAsync(0L);
            return;
        }
        CompletableFuture.supplyAsync(() -> {

            return null;
        }).thenAccept(
                //回调里面更新用户的运动记录
                action -> RecordRepository.INSTANCE.refreshRecordAsync(user.getId())
        );
    }
}