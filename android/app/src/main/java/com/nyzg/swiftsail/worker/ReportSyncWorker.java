package com.nyzg.swiftsail.worker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.bean.MyJsonSerializer;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.bean.ServerURL;
import com.nyzg.swiftsail.dao.RecordTable;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.netobj.report.DayRecord;
import com.nyzg.swiftsail.netobj.report.LongSyncResp;
import com.nyzg.swiftsail.obj.Pair;
import com.nyzg.swiftsail.repository.LoginRepository;
import com.nyzg.swiftsail.repository.SyncRepository;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class ReportSyncWorker extends Worker {
    public ReportSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    private enum Status {
        ERROR, CLOSE, HEAR_BEAT
    }

    private static class Message implements Delayed {
        long delay;
        Status status;

        public Message(long delay, @NonNull Status status) {
            this.delay = delay;
            this.status = status;
        }

        public Status getStatus() {
            return status;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return delay - System.currentTimeMillis();
        }

        @Override
        public int compareTo(Delayed other) {
            if (other == this) return 0;
            long diff = this.delay - ((Message) other).delay;
            return (diff > 0) ? 1 : (diff < 0) ? -1 : 0;
        }
    }

    final private DelayQueue<Message> delayQueue = new DelayQueue<>();

    @NonNull
    @Override
    public Result doWork() {
        User user = LoginRepository.getInstance().currentUser.getValue();
        if (user == null || user.id == 0L) {//早不到用户或者本地用户，直接返回
            return Result.success();
        }

        WebSocketClient webSocketClient = getWebSocketClient();
        try {
            webSocketClient.connectBlocking();
        } catch (Exception ignore) {
            return Result.failure();
        }
        boolean successLeave = true;
        final long heartBeatInterval = 25 * 1000;
        delayQueue.add(new Message(System.currentTimeMillis() + heartBeatInterval, Status.HEAR_BEAT));
        while (!isStopped()) {
            try {
                Message message = delayQueue.take();
                Status status = message.getStatus();
                if (status == Status.ERROR) {
                    successLeave = false;
                    break;
                } else if (status == Status.CLOSE) {
                    break;
                } else if (status == Status.HEAR_BEAT) {
                    webSocketClient.sendPing();
                    //25s后继续跳一次
                    delayQueue.add(new Message(System.currentTimeMillis() + heartBeatInterval, Status.HEAR_BEAT));
                }
            } catch (Exception ignore) {
                return Result.failure();
            }
        }
        try {
            webSocketClient.closeBlocking();
        } catch (InterruptedException e) {
            return Result.failure();
        }
        return successLeave ? Result.success() : Result.failure();
    }

    /**
     * 交互流程：
     * 客户端->服务端 tk token tkABC.DEF.GHI
     * 服务端->客户端 rg range rg%d|%d
     * 客户端->服务端 mr my range mr%d|%d
     * 服务端->客户端 dt data dt{json-LongSyncResp}
     * 客户端接收完数据，关闭连接
     */
    @NonNull
    private WebSocketClient getWebSocketClient() {
        return new WebSocketClient(ServerURL.URI_WEBSOCKET_REPORT_SYNC) {
            private long userId;
            final private Message MESSAGE_ERROR = new Message(0, Status.ERROR);
            final private Message MESSAGE_CLOSE = new Message(0, Status.CLOSE);
            private final List<Pair<Long, Long>> rangePair = new ArrayList<>();
            private int rangePointer = 0;
            private int totalCount = 0;
            private int currentCount = 0;

            @Override
            public void onOpen(ServerHandshake handshakeData) {
                User user = LoginRepository.getInstance().currentUser.getValue();
                if (user == null) {
                    delayQueue.add(MESSAGE_ERROR);
                    return;
                }
                userId = user.id;
                String token = user.token;
                send("tk" + token);
            }

            @Override
            public void onMessage(String message) {
                if (message.length() < 2) {
                    delayQueue.add(MESSAGE_ERROR);
                    return;
                }
                String prefix = message.substring(0, 2);
                String payload = message.substring(2);
                switch (prefix) {
                    case "rg" -> handleRg(payload);//只会执行一次
                    case "dt" -> handleDt(payload);
                    default -> delayQueue.add(MESSAGE_ERROR);
                }
            }

            private void handleRg(String payload) {
                try {
                    //选出自己没有的数据，放到rangePair里面
                    String[] range = payload.split("\\|");
                    long from = Long.parseLong(range[0]);
                    long to = Long.parseLong(range[1]);
                    //epochList按照epochDay升序排列
                    List<Long> epochList = SQLiteDB.getDatabase(GlobalApplication.getAppContext())
                            .recordTable().getEpochDay(userId);
                    if (epochList.isEmpty()) {
                        putDays(from, to);
                    } else {
                        int j = 0;
                        long pre = from;
                        for (long i = from; i <= to; ) {
                            if (j >= epochList.size()) {
                                putDays(pre, to);
                                break;
                            } else {
                                long e = epochList.get(j);
                                if (i < e) {
                                    i = Math.min(to, e);
                                    if (i == to && e != to) {
                                        putDays(pre, to);
                                        break;
                                    } else {
                                        putDays(pre, i - 1);
                                    }
                                } else if (i == e) {
                                    ++i;
                                    pre = i;
                                    ++j;
                                } else {
                                    pre = i;
                                    ++j;
                                }
                            }
                        }
                    }
                    Log.v("myTag", "开始同步数据" + from + " " + to + " totalCount=" + totalCount+" pairSize:"+rangePair.size());
                    //发送第一个mr
                    sendMr();
                    //更新UI为progress=0的状态
                    SyncRepository.getInstance().totalSyncProgress.postValue(0f);
                } catch (Exception e) {
                    delayQueue.add(MESSAGE_ERROR);
                }
            }

            private void putDays(long from, long to) {
                totalCount += (int) (to-from + 1);
                for (long i = from; i <= to; i += 30) {
                    if (i + 29 >= to) {
                        rangePair.add(new Pair<>(i, to));
                        break;
                    }
                    rangePair.add(new Pair<>(i, i + 29));
                }
            }

            @SuppressLint("DefaultLocale")
            private void handleDt(String payload) {
                try {
                    RecordTable recordTable = SQLiteDB.getDatabase(GlobalApplication.getAppContext()).recordTable();
                    //处理数据
                    LongSyncResp resp = MyJsonSerializer.deSerialize(payload, LongSyncResp.class);
                    for (DayRecord dayRecord : resp.dayRecordList) {
                        recordTable.insertRecordList(dayRecord.recordList);
                        currentCount += dayRecord.recordList.size();
                    }
                    //提醒UI发生变化
                    Log.v("myTag", "收到mr" + currentCount);
                    SyncRepository.getInstance().totalSyncProgress.setValue(totalCount != 0 ? currentCount / (float) totalCount : 0f);
                    //然后看情况是发送下一个还是直接关闭连接
                    sendMr();
                } catch (Exception e) {
                    delayQueue.add(MESSAGE_ERROR);
                }
            }

            @SuppressLint("DefaultLocale")
            private void sendMr() {
                if (rangePointer >= rangePair.size()) {
                    delayQueue.add(MESSAGE_CLOSE);
                }
                Pair<Long, Long> pair = rangePair.get(rangePointer);
                ++rangePointer;
                send(String.format("mr%d|%d", pair.getA(), pair.getB()));
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                if (code == 1000) { // 正常关闭
                    delayQueue.add(new Message(0, Status.CLOSE));
                    Log.v("myTag","数据同步完成，安全关闭");
                } else {
                    Log.v("myTag", "websocket关闭，code: " + code);
                    delayQueue.add(MESSAGE_ERROR);
                }
            }

            @Override
            public void onError(Exception ex) {
                delayQueue.add(MESSAGE_ERROR);
            }
        };
    }
}