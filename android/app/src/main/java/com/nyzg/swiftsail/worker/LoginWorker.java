package com.nyzg.swiftsail.worker;

import android.content.Context;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.MainActivity;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.dao.UserTable;
import com.nyzg.swiftsail.dbobj.User;

import java.util.List;

public class LoginWorker extends Worker {
    public LoginWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        SQLiteDB sqLiteDB = SQLiteDB.getDatabase(GlobalApplication.getAppContext());
        UserTable userTable = sqLiteDB.userTable();
        List<User> userList = userTable.selectAllUser();
        if (userList.isEmpty()) {//本地账户为空
            Message showRegisterMsg = Message.obtain();
            showRegisterMsg.what = MainActivity.SHOW_REGISTER_MSG;
            MainActivity.getHandler().sendMessage(showRegisterMsg);
            return Result.success();
        }
        Message showLoginMsg = Message.obtain();
        showLoginMsg.what = MainActivity.SHOW_LOGIN_MSG;
        showLoginMsg.obj = userList;
        MainActivity.getHandler().sendMessage(showLoginMsg);
        return Result.success();
    }
}
