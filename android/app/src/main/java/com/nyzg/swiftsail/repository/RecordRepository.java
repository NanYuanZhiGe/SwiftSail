package com.nyzg.swiftsail.repository;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.bean.GlobalToast;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.dao.RecordTable;
import com.nyzg.swiftsail.dbobj.Record;
import com.nyzg.swiftsail.obj.RecordData;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RecordRepository {
    private static final Type TYPE_MAP = new TypeToken<Map<String, Object>>() {
    }.getType();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
    public final static RecordRepository INSTANCE = new RecordRepository();
    private final MutableLiveData<RecordData> mutableRecordData = new MutableLiveData<>();

    private RecordRepository() {
    }

    public void refreshRecordAsync(long userId) {
        CompletableFuture.supplyAsync(() -> {
            SQLiteDB sqLiteDB = SQLiteDB.getDatabase(GlobalApplication.getAppContext());
            RecordTable recordTable = sqLiteDB.recordTable();
            List<Record> recordList = recordTable.selectTodayData(LocalDate.now().toEpochDay(), userId);
            parseJsonAndSet(recordList);
            return null;
        });
    }

    //update是加上基础制
    public void updateData(boolean useFeet, int startHour, int endHour, float meters, int durationSecond) {
        RecordData recordData;
        if (mutableRecordData.getValue() == null) {
            recordData = new RecordData();
        } else {
            recordData = new RecordData(mutableRecordData.getValue());
        }
        if (useFeet) {
            recordData.updateFeetData(startHour, endHour, meters, durationSecond);
        } else {
            recordData.updateWheelData(startHour, endHour, meters, durationSecond);
        }
        mutableRecordData.postValue(recordData);
    }

    //set是拿新值来替代旧值
    public void setData(boolean useFeet, int startHour, int endHour, float meters, int durationSecond) {
        RecordData recordData;
        recordData = new RecordData();
        if (useFeet) {
            recordData.setFeetData(startHour, endHour, meters, durationSecond);
        } else {
            recordData.setWheelData(startHour, endHour, meters, durationSecond);
        }
        mutableRecordData.postValue(recordData);
    }


    public void submitRepositoryRecord(Record record) {
        RecordTable recordTable = SQLiteDB.getDatabase(GlobalApplication.getAppContext()).recordTable();
        CompletableFuture.supplyAsync(() -> {
            try {
                recordTable.insertRecord(record);
            } catch (Exception e) {
                return 1;
            }
            return 0;
        }).thenAccept(code -> {
            if (code == 1) {
                GlobalToast.COMMON_TOAST.accept("写入本地数据库失败，取消云同步");
                return;
            }
            //更新UI数据
            List<Record> newRecord = new ArrayList<>(1);
            newRecord.add(record);
            parseJsonAndUpdate(newRecord);
            if (record.userId == 0L) {//是本地用户，直接返回就行了
                return;
            }
            //否则就需要向云端同步数据
        });
    }

    public void parseJsonAndSet(List<Record> recordList) {
        parseJsonAndDoFunction(recordList, true);
    }

    public void parseJsonAndUpdate(List<Record> recordList) {
        parseJsonAndDoFunction(recordList, false);
    }

    private void parseJsonAndDoFunction(List<Record> recordList, boolean isSetData) {
     /*
        {
            "type":"useFeet"/"useWheel",
            "duration":100 ->double,
            "distance":10 -> double ,
            "startTime":"yyyy:MM:dd HH:mm:ss",
            "endTime":"yyyy:MM:dd HH:mm:ss"
        }
         */
        for (Record record : recordList) {
            String json = record.record;
            if (json == null) {
                continue;
            }
            Map<String, Object> map = new Gson().fromJson(json, TYPE_MAP);
            String type = (String) map.get("type");
            if (type == null || (!type.equals("useFeet") && !type.equals("useWheel"))) {
                continue;
            }
            try {
                LocalDateTime startTime = LocalDateTime.parse((String) map.get("startTime"), DATE_TIME_FORMATTER);
                LocalDateTime endTime = LocalDateTime.parse((String) map.get("endTime"), DATE_TIME_FORMATTER);
                Double meters = (Double) map.get("distance");
                if (meters == null) {
                    continue;
                }
                Double duration = (Double) map.get("duration");
                if (duration == null) {
                    continue;
                }
                if (isSetData) {
                    this.setData(type.equals("useFeet"),
                            startTime.getHour(),
                            endTime.getHour(),
                            meters.floatValue(),
                            duration.intValue());
                    //第一次就set，后面的就是update
                    isSetData = false;
                } else {
                    this.updateData(type.equals("useFeet"),
                            startTime.getHour(),
                            endTime.getHour(),
                            meters.floatValue(),
                            duration.intValue());
                }
            } catch (Exception e) {
                continue;
            }
        }
    }

    public LiveData<RecordData> getLiveRecordData() {
        return mutableRecordData;
    }
}
