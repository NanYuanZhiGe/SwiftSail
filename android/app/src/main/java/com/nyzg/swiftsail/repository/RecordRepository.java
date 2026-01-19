package com.nyzg.swiftsail.repository;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.bean.GlobalToast;
import com.nyzg.swiftsail.bean.NetWorkBuilder;
import com.nyzg.swiftsail.bean.NetWorkHandler;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.bean.ServerURL;
import com.nyzg.swiftsail.dao.RecordBackUpTable;
import com.nyzg.swiftsail.dbobj.RecordBackUp;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.netobj.record.SyncRecordBackUpReq;
import com.nyzg.swiftsail.obj.RecordData;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import okhttp3.Request;
import okhttp3.Response;

public class RecordRepository {
    private static final Type TYPE_MAP = new TypeToken<Map<String, Object>>() {
    }.getType();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
    private volatile static RecordRepository INSTANCE=null;

    //这个成员核心，它主要是用于RecordFragment的UI显示
    private final MutableLiveData<RecordData> mutableRecordData = new MutableLiveData<>();
    private final ReadWriteLock recordLock = new ReentrantReadWriteLock();
    private final AtomicBoolean OBSERVE_USER_LOCK = new AtomicBoolean(false);

    public static RecordRepository getInstance() {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        synchronized (RecordRepository.class) {
            if (INSTANCE != null) {
                return INSTANCE;
            }
            INSTANCE = new RecordRepository();
        }
        return INSTANCE;
    }

    private RecordRepository() {
    }

    public void updateUserOnChange() {
        if (!OBSERVE_USER_LOCK.compareAndSet(false, true)) {
            return;
        }
        //由于LoginRepository的currentUser通过postValue的方式更新，所以执行的速度会比worker慢
        //导致user为空然后就没有办法更新用户数据，所以需要通过observe的方式进行数据更新
        //然后每次observe的触发，都必须进行全量更新，此时应该锁住所有的写操作
        LoginRepository.getInstance().getMutableCurrentUser().observeForever(this::initUserRecordDataAsync);
    }

    /**
     * 根据当前用户重新加载一遍用户的运动记录
     *
     * @param user 当前用户
     */
    public void initUserRecordDataAsync(User user) {
        if (user == null) {
            return;
        }
        CompletableFuture.supplyAsync(() -> {
            //首先获取写锁
            recordLock.writeLock().lock();
            RecordBackUpTable recordBackUpTable = SQLiteDB.getDatabase(GlobalApplication.getAppContext()).recordBackUpTable();
            try {
                List<RecordBackUp> recordBackUpList = recordBackUpTable.selectTodayData(LocalDate.now().toEpochDay(), user.id);
                parseJsonAndSet(recordLock, recordBackUpList);
                //从数据库中查询该用户今天的所有数据，然后更新
            } catch (Exception ignored) {//有异常就不理，提醒一下用户
                GlobalToast.COMMON_TOAST.accept("读取RecordBackUpTable异常");
            } finally {
                recordLock.writeLock().unlock();
            }
            return null;
        });
    }

    public void submitRepositoryRecordAsync(RecordBackUp recordBackUp) {
        RecordBackUpTable recordBackUpTable = SQLiteDB.getDatabase(GlobalApplication.getAppContext()).recordBackUpTable();
        CompletableFuture.supplyAsync(() -> {
            try {
                recordBackUpTable.insertRecordBackUp(recordBackUp);
            } catch (Exception e) {
                GlobalToast.COMMON_TOAST.accept("写入本地数据库失败，取消云同步");
                return null;
            }
            //更新UI数据
            List<RecordBackUp> newRecord = new ArrayList<>(1);
            newRecord.add(recordBackUp);
            parseJsonAndUpdate(recordLock, newRecord);
            //是本地用户，直接返回就行了
            if (recordBackUp.userId == 0L) {
                return null;
            }
            //否则就需要向云端同步数据
            //同步数据时select所有sync=0的数据进行同步
            List<RecordBackUp> recordList = recordBackUpTable.selectAllUnSync(recordBackUp.userId);
            SyncRecordBackUpReq req = new SyncRecordBackUpReq();
            req.recordList = recordList;
            Request request = NetWorkBuilder.buildJsonRequestJwt(
                    ServerURL.URL_SYNC_BACKUP_RECORD,
                    ServerURL.POST,
                    req
            );
            Response response = NetWorkBuilder.doChunkRequest(request);
            NetWorkHandler.handleNetRespAfterLogin(
                    null, response,
                    () -> GlobalToast.COMMON_TOAST.accept("云同步运动记录失败，下次提交自动同步"),
                    httpResp -> {
                        //请求成功，更新本地数据为sync=1
                        List<String> recordIds = new ArrayList<>(recordList.size());
                        for (RecordBackUp temp : recordList) {
                            recordIds.add(temp.recordId);
                        }
                        recordBackUpTable.updateDataAsSync(recordIds);
                        GlobalToast.COMMON_TOAST.accept("运动记录云同步成功");
                    }, Void.class);

            return null;
        });
    }


    private void parseJsonAndSet(ReadWriteLock lock, List<RecordBackUp> recordBackUpList) {
        lock.writeLock().lock();
        try {
            parseJsonAndDoFunction(recordBackUpList, true);
        } catch (Exception ignore) {
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void parseJsonAndUpdate(ReadWriteLock lock, List<RecordBackUp> recordBackUpList) {
        lock.writeLock().lock();
        try {
            parseJsonAndDoFunction(recordBackUpList, false);
        } catch (Exception ignore) {
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void parseJsonAndDoFunction(List<RecordBackUp> recordBackUpList, boolean isSetData) {
     /*
        {
            "type":"useFeet"/"useWheel",
            "duration":100 ->double,
            "distance":10 -> double ,
            "startTime":"yyyy:MM:dd HH:mm:ss",
            "endTime":"yyyy:MM:dd HH:mm:ss"
        }
         */
        for (RecordBackUp recordBackUp : recordBackUpList) {
            String json = recordBackUp.detailValue;
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
                    this.setChartData(type.equals("useFeet"),
                            startTime.getHour(),
                            endTime.getHour(),
                            meters.floatValue(),
                            duration.intValue());
                    //第一次就set，后面的就是update
                    isSetData = false;
                } else {
                    this.updateChartData(type.equals("useFeet"),
                            startTime.getHour(),
                            endTime.getHour(),
                            meters.floatValue(),
                            duration.intValue());
                }
            } catch (Exception ignore) {//一般是json解析出错，不用理，继续循环
            }
        }
    }

    //update是加上基础制
    private void updateChartData(boolean useFeet, int startHour, int endHour, float meters, int durationSecond) {
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
    private void setChartData(boolean useFeet, int startHour, int endHour, float meters, int durationSecond) {
        RecordData recordData;
        recordData = new RecordData();
        if (useFeet) {
            recordData.setFeetData(startHour, endHour, meters, durationSecond);
        } else {
            recordData.setWheelData(startHour, endHour, meters, durationSecond);
        }
        mutableRecordData.postValue(recordData);
    }

    public LiveData<RecordData> getLiveRecordData() {
        return mutableRecordData;
    }
}
