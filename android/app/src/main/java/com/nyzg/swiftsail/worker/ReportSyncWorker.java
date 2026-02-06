package com.nyzg.swiftsail.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.bean.DateUtils;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.bean.MyJsonSerializer;
import com.nyzg.swiftsail.bean.NetWorkBuilder;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.bean.ServerURL;
import com.nyzg.swiftsail.dao.RecordTable;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.netobj.report.Watch;
import com.nyzg.swiftsail.netobj.report.AcquireSyncDataReq;
import com.nyzg.swiftsail.netobj.report.DayRecord;
import com.nyzg.swiftsail.obj.Pair;
import com.nyzg.swiftsail.repository.LoginRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Response;
import okio.BufferedSource;

public class ReportSyncWorker extends Worker {
    public ReportSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        User user = LoginRepository.getInstance().currentUser.getValue();
        if (user == null || user.id == 0L) {//早不到用户或者本地用户，直接返回
            return Result.success();
        }
        /*
        OkHttpClient okHttpClient = GlobalInstance.OK_HTTP_NO_PROXY;

        try {
            Watch watch = SQLiteDB.getDatabase(GlobalApplication.getAppContext()).watchTable().selectWatchActivated(user.id);
            if (watch == null || watch.clientId == null || watch.clientId.isEmpty()) {//如果没有可用的watch
                return Result.success();
            }
            RecordTable recordTable = SQLiteDB.getDatabase(GlobalApplication.getAppContext()).recordTable();
            //有数据的天数，内部去重，每一个epochDay各不相同，按照epochDay升序排列
            List<Long> containedDay = recordTable.getEpochDay(user.id);
            //同步的初始日为手表添加的那一天
            long createEpochDay = Math.max(watch.createEpoch, DateUtils.MIN_CREATE_DATE);
            //同步的结束日为今天
            long nowEpochDay = LocalDate.now().toEpochDay();
            long i = createEpochDay;
            List<Pair<Long, Long>> leakDataList = new ArrayList<>();
            //过滤出本地没有数据的天数
            for (int j = 0; j < containedDay.size(); ++j) {
                long temp = containedDay.get(j);
                if (temp == i) {
                    ++i;
                    continue;
                }
                if (temp - 1 >= i) {
                    leakDataList.add(new Pair<>(i, temp - 1));
                }
                i = temp + 1;
            }
            if (i <= nowEpochDay) {
                leakDataList.add(new Pair<>(i , nowEpochDay));
            }
            Response response = null;
            try {
                response = okHttpClient.newCall(NetWorkBuilder.buildJsonRequestJwt(
                        ServerURL.URL_ACQUIRE_SYNC_DATA, ServerURL.POST, new AcquireSyncDataReq(watch.clientId, leakDataList)
                )).execute();
                if (!response.isSuccessful() || response.code() != 200) {
                    response.close();
                    return Result.failure();
                }
                if (response.body() == null) {//认为没有数据需要同步
                    response.close();
                    return Result.success();
                }
                BufferedSource source = response.body().source();
                while (!source.exhausted()) {
                    String line = source.readUtf8Line(); // 读一行（不含 \n）
                    if (line != null) {
                        line = line.trim();
                    }
                    if (line == null || line.isEmpty()) {
                        continue;
                    }
                    //{"_eof":true}标记为结束
                    if ("{\"_eof\":true}".equals(line) || "null".equals(line)) {
                        Log.d("myTag", "Received EOF marker, sync complete.");
                        break; // 正常结束
                    }

                    // 处理有效 JSON 行
                    DayRecord dayRecord = MyJsonSerializer.deSerialize(line, DayRecord.class);
                    if (dayRecord == null || dayRecord.recordList == null || dayRecord.recordList.isEmpty()) {
                        continue;
                    }
                    //批量写入每天的数据
                    SQLiteDB.getDatabase(GlobalApplication.getAppContext())
                            .recordTable()
                            .insertRecordList(dayRecord.recordList);
                }
            } catch (Exception e) {
                if (response != null) {
                    response.close();
                }
                return Result.failure();
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        } catch (Exception e) {
            return Result.failure();
        }
         */
        return Result.success();
    }
}
