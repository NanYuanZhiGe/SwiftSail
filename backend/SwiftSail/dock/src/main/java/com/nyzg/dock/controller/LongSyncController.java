package com.nyzg.dock.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nyzg.common.Pair;
import com.nyzg.common.ss_utils.JwtThreadSafe;
import com.nyzg.dock.dbobj.Record;
import com.nyzg.dock.dbobj.Watch;
import com.nyzg.dock.netobj.AcquireSyncDataReq;
import com.nyzg.dock.netobj.DayRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 整个的交互过程是这样子的：
 * 客户端建立websocket连接
 * 客户端发送tk头，包含token，然后客户端等待服务端响应
 * 服务端返回al头或者是er头，er头客户端关闭连接
 * 客户端得到al头之后，就发送rq头，只发一次，之后服务端就不断返回数据给客户端，直到发送ed
 * 期间客户端不断发送ping包给服务端来保持nat
 */
@Slf4j
@Component
@Scope("prototype")
public class LongSyncController extends TextWebSocketHandler {
    private final byte[] hsKey;
    @Resource
    private JdbcTemplate jdbcTemplate;

    public LongSyncController(@Value("${jwt.hs-key:hello-world}") String strHsKey) {
        hsKey = strHsKey.getBytes(StandardCharsets.UTF_8);
    }

    private boolean isValidate = false;
    private String email;
    private long userId;
    private static final int NO_VALIDATE_TOKEN = 2000;
    private static final int DESERIALIZE_ERROR = 3000;
    private static final int UNKNOWN_ERROR = 4000;
    private static final int SERVER_TO_BUSY = 5000;
    private static final Executor THREAD_POOL = new ThreadPoolExecutor(
            2, Runtime.getRuntime().availableProcessors() * 2,
            5 * 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(50),
            new ThreadFactory() {
                final private AtomicInteger count = new AtomicInteger(0);

                @Override
                public Thread newThread(@NotNull Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("LongSyncControllerThread-" + count.getAndIncrement());
                    return thread;
                }
            },
            new ThreadPoolExecutor.AbortPolicy()
    );
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final AtomicReference<Runnable> myThread = new AtomicReference<>(null);

    //心跳包
    @Override
    protected void handlePongMessage(@NotNull WebSocketSession session, @NotNull PongMessage message) throws Exception {
        super.handlePongMessage(session, message);
        //暂时什么都不做
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, @NotNull TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        String text = message.getPayload();
        if (text.startsWith("tk")) {//token，进行验证
            String token = text.substring(2);
            Pair<JwtThreadSafe.Status, Map<String, Object>> statusMapPair = JwtThreadSafe.validateJwtTokenHMac(token, hsKey);
            if (statusMapPair.getA() == JwtThreadSafe.Status.SUCCESS) {
                isValidate = true;
                session.sendMessage(new TextMessage("al"));//allow
                email = (String) statusMapPair.getB().get("email");
                userId = Long.parseLong((statusMapPair.getB().get("userId")).toString());
                return;
            }
            if (statusMapPair.getA() == JwtThreadSafe.Status.EXPIRE) {
                session.sendMessage(new TextMessage("erTokenExpire"));
            } else if (statusMapPair.getA() == JwtThreadSafe.Status.FAIL) {
                session.sendMessage(new TextMessage("erTokenNotValidate"));
            }
            session.close(new CloseStatus(NO_VALIDATE_TOKEN));
            return;
        }
        if (!isValidate) {
            session.close(new CloseStatus(NO_VALIDATE_TOKEN));
            return;
        }
        if (!text.startsWith("rq")) {//其他的不支持数据
            return;
        }
        //客户端只会请求一次，其中rq包含了客户端已经有的数据，之后就是我们发数据给客户端了
        AcquireSyncDataReq req;
        try {
            req = OBJECT_MAPPER.readValue(text.substring(2), AcquireSyncDataReq.class);
        } catch (Exception e) {
            log.error("WebSocket反序列化错误, 用户的email：" + email);
            session.close(new CloseStatus(DESERIALIZE_ERROR));
            return;
        }
        //获取用户当前activate的手表
        long currentTime = System.currentTimeMillis();
        Watch watch = jdbcTemplate.query("""
                        SELECT `clientId`,`createTime` FROM `watchTable` WHERE `userId`=? AND `expireTime`>? AND `activate`=1;
                        """, new BeanPropertyRowMapper<>(Watch.class), userId, currentTime)
                .stream().findFirst().orElse(null);
        Runnable r;
        if (watch == null) {//如果watch是空的，就根据现有的record来过滤，
            r = new EmptyWatchRunnable(session, req, userId);
        } else {//如果watch不是空的，就进行网络请求来返回数据
            r = new ActivateWatchRunnable(session, req, userId, watch.getClientId(), watch.getCreateTime().toLocalDate().toEpochDay());
        }
        if (myThread.compareAndSet(null, r)) {//不要多次启动
            try {
                THREAD_POOL.execute(myThread.get());
            } catch (Exception e) {//线程池可能拒绝
                session.close(new CloseStatus(SERVER_TO_BUSY));
            }
        }
    }

    @Override
    public void handleTransportError(@NotNull WebSocketSession session, @NotNull Throwable exception) throws Exception {
        super.handleTransportError(session, exception);
        log.error(String.format("email为%s 的数据同步出现异常", email));
        session.close(new CloseStatus(UNKNOWN_ERROR));
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
    }

    private class EmptyWatchRunnable extends BaseRunnable {
        public EmptyWatchRunnable(WebSocketSession session, AcquireSyncDataReq req, long userId) {
            super(session, req, userId);
        }

        /**
         * 用户的可用手表为空，所以我们就把已经有的数据同步给用户
         */
        @Override
        public void run() {
            try {
                List<Long> epochList = getAllEpochDay();
                //如果用户返回的是空的
                //就把已有的数据全部给它
                if (req.getContainedDataList() == null || req.getContainedDataList().isEmpty()) {
                    //分页查询并返回数据
                    for (int i = 0; i < epochList.size(); i += 50) {
                        int next = i + 49;
                        if (next >= epochList.size()) {
                            next = epochList.size() - 1;
                        }
                        long epoch1 = epochList.get(i);
                        long epoch2 = epochList.get(next);
                        List<Record> recordList = jdbcTemplate.query("""
                                SELECT * FROM `recordTable` WHERE `userId`=? AND `epochDay` BETWEEN ? AND ? ORDER BY `epochDay`;
                                """, new BeanPropertyRowMapper<>(Record.class), userId, epoch1, epoch2);
                        //按每天进行分类
                        if (sendDataBackFail(recordList)) {
                            return;
                        }
                    }
                } else {
                    //如果用户自己里面有一点数据，我们就需要返回用户没有的
                    //这里使用双指针
                    //找到所有位于epochList但是不位于req中区间数组中的epochDay
                    List<Long> resultList = new ArrayList<>();
                    int i = 0;
                    int size = req.getContainedDataList().size();
                    for (int j = 0; ; ) {
                        if (i >= epochList.size()) {//如果epochList为空，那么它自然第一次循环就会退出
                            break;
                        }
                        long e = epochList.get(i);
                        Pair<Long, Long> pair = req.getContainedDataList().get(j);
                        long start = pair.getA();
                        long end = pair.getB();
                        if (e < start) {//小于，这个区间，需要加进去
                            resultList.add(e);
                            ++i;
                        } else if (e <= end) {//在[start,end]之间，不需要
                            ++i;
                        } else if (j < size - 1) {//大于这个区间，需要查看下一个区间
                            ++j;
                        } else {//已经没有剩下的区间了
                            resultList.add(e);
                            ++i;
                        }
                    }
                    //从数据库中查询数据然后返回
                    i = 0;
                    for (int offset = 49; i < resultList.size(); ) {
                        if (i + offset >= resultList.size()) {
                            offset = resultList.size() - i - 1;
                        }
                        boolean continuous = true;
                        for (int k = i + 1; k <= i + offset; ++k) {
                            if (resultList.get(k) != resultList.get(k - 1) + 1) {
                                continuous = false;
                                break;
                            }
                        }
                        if (continuous) {//如果是连续的，就使用between
                            List<Record> recordList = jdbcTemplate.query("""
                                    SELECT * FROM `recordTable` WHERE `userId`=? AND `epochDay` BETWEEN ? AND ? ORDER BY `epochDay`;
                                    """, new BeanPropertyRowMapper<>(Record.class), userId, resultList.get(i), resultList.get(i + offset));
                            if (sendDataBackFail(recordList)) {
                                return;
                            }
                        } else {//如果不是连续的，就是用in
                            List<Long> subList = resultList.subList(i, i + offset + 1);
                            String placeholders = subList.stream()
                                    .map(day -> "?")
                                    .collect(Collectors.joining(", "));
                            String sql = "SELECT * FROM `recordTable` WHERE `userId`=? AND `epochDay` IN (" + placeholders + ") ORDER BY `epochDay`;";
                            List<Record> recordList = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Record.class), userId, subList.toArray());
                            if (sendDataBackFail(recordList)) {
                                return;
                            }
                        }
                        i += (offset + 1);
                    }
                }
            } catch (Exception e) {
                log.info(String.format("email为%s 在同步数据时遇到错误%s", email, e.getCause()));
                try {
                    session.close(new CloseStatus(UNKNOWN_ERROR));
                } catch (Exception io) {
                    log.error(String.format("email为%s 遇到其他类型的错误，关闭session错误%s", email, e.getCause()));
                }
                return;
            }
            //结束session
            try {
                session.sendMessage(new TextMessage("ed"));
                session.close(new CloseStatus(1000));
            } catch (Exception e) {
                log.info(String.format("email为%s 在结束session的时候出现错误%s", email, e.getCause()));
            }
        }
    }

    private class ActivateWatchRunnable extends BaseRunnable {
        private final String clientId;
        private final long createEpoch;

        public ActivateWatchRunnable(WebSocketSession session, AcquireSyncDataReq req, long userId, String clientId, long createEpoch) {
            super(session, req, userId);
            this.clientId = clientId;
            this.createEpoch = createEpoch;
        }

        /**
         * 用户可用手表不为空，所以我们就根据手表的创建日期和今天来同步数据
         * 如果我们的数据库中有这个数据我们能就直接返回给用户
         * 如果我们的数据库中没有这个数据，而且手表的有效期还在，我们就网络请求这个数据
         */
        @Override
        public void run() {
            long currentEpoch = LocalDate.now().toEpochDay();
            try {
                List<Long> epochList = getAllEpochDay();
                /*
                这里的逻辑分为两步，第一步，查询出用户没有，数据库中没有的数据，这部分数据进行网络请求拿到，然后返回给用户。
                第二部分，查询出用户没有，数据库中有的数据，分页查询拿到，然后返回给用户。
                所以首先是拿到两个epoch列表
                 */
                List<Long> listForNet = new ArrayList<>();
                List<Long> listForDb = new ArrayList<>();
                //用户什么数据都没有
                if (req.getContainedDataList() == null || req.getContainedDataList().isEmpty()) {
                    listForDb = epochList;
                    //不要同步今天的数据，所以用<号而不是<=
                    int j = 0;
                    for (long i = createEpoch; i < currentEpoch; ) {
                        if (j < epochList.size()) {
                            long e = epochList.get(j);
                            if (e > i) {
                                listForNet.add(i);
                                ++i;
                            } else if (e < i) {
                                ++j;
                            }
                        } else {
                            listForNet.add(i);
                            ++i;
                        }
                    }
                } else {//用户有部分数据


                }
            } catch (Exception e) {
                log.info(String.format("email为%s 在同步数据时遇到错误%s", email, e.getCause()));
                try {
                    session.close(new CloseStatus(UNKNOWN_ERROR));
                } catch (Exception io) {
                    log.error(String.format("email为%s 遇到其他类型的错误，关闭session错误%s", email, e.getCause()));
                }
                return;
            }
            //结束session
            try {
                session.sendMessage(new TextMessage("ed"));
                session.close(new CloseStatus(1000));
            } catch (Exception e) {
                log.info(String.format("email为%s 在结束session的时候出现错误%s", email, e.getCause()));
            }
        }
    }

    private abstract class BaseRunnable implements Runnable {
        final protected WebSocketSession session;
        final protected AcquireSyncDataReq req;
        final protected long userId;


        public BaseRunnable(WebSocketSession session, AcquireSyncDataReq req, long userId) {
            this.session = session;
            this.req = req;
            this.userId = userId;
        }

        @NotNull
        protected List<Long> getAllEpochDay() {
            return jdbcTemplate.query("""
                    SELECT `epochDay` FROM `recordTable` WHERE `userId`=? ORDER BY `epochDay`;
                    """, new SingleColumnRowMapper<>(Long.class), userId);
        }

        /**
         * @return 出现异常返回false，此时你直接return就行了，里面自动session close
         */
        protected boolean sendDataBackFail(List<Record> recordList) {
            if (recordList == null) {
                return false;
            }
            List<DayRecord> dayRecordList = new ArrayList<>(recordList.stream()
                    .collect(Collectors.groupingBy(
                            Record::getEpochDay,
                            Collectors.collectingAndThen(
                                    Collectors.toList(),
                                    list -> {
                                        DayRecord dr = new DayRecord();
                                        dr.getRecordList().addAll(list);
                                        return dr;
                                    }
                            )
                    )).values());
            for (DayRecord dayRecord : dayRecordList) {
                if (!session.isOpen()) {//如果session关闭了
                    return true;
                }
                try {
                    session.sendMessage(new TextMessage(String.format("rs%s", OBJECT_MAPPER.writeValueAsString(dayRecord))));
                } catch (Exception e) {
                    log.error(String.format("email为%s 返回数据的时候出现序列化错误%s", email, e.getCause()));
                    try {
                        session.close(new CloseStatus(DESERIALIZE_ERROR));
                    } catch (Exception io) {
                        log.error(String.format("email为%s 反序列化错误，关闭session错误%s", email, e.getCause()));
                    }
                    return true;
                }
            }
            return false;
        }
    }
}