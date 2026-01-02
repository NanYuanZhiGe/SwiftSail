package com.nyzg.swiftsail.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.dao.RecordTable;
import com.nyzg.swiftsail.dbobj.Record;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.repository.LoginRepository;
import com.nyzg.swiftsail.repository.RecordRepository;

import java.time.LocalDate;
import java.util.List;

public class RecordBarchartWorker extends Worker {
    public RecordBarchartWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * 从数据库中读取当前用户的今日的运动信息，然后更新今日的运动图表，然后返回
     */
    @NonNull
    @Override
    public Result doWork() {
        RecordRepository recordRepository = RecordRepository.INSTANCE;
        RecordTable recordTable = SQLiteDB.getDatabase(GlobalApplication.getAppContext()).recordTable();
        User currentUser = LoginRepository.INSTANCE.getMutableCurrentUser().getValue();
        if (currentUser == null) {
            return Result.success();
        }
        List<Record> recordList = recordTable.selectTodayData(LocalDate.now().toEpochDay(), currentUser.getId());
        recordRepository.parseJsonAndSet(recordList);
        return Result.success();
    }
}
