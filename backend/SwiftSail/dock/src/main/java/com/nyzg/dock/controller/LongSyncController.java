package com.nyzg.dock.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nyzg.common.Pair;
import com.nyzg.common.ss_utils.JwtThreadSafe;
import com.nyzg.dock.dbobj.Record;
import com.nyzg.dock.dbobj.Watch;
import com.nyzg.dock.netobj.DayRecord;
import com.nyzg.dock.netobj.LongSyncResp;
import com.nyzg.dock.service.FitbitWebApiService;
import jakarta.annotation.Nullable;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 */
@Slf4j
@Component
@Scope("prototype")
public class LongSyncController extends TextWebSocketHandler {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private record Param(WebSocketSession session, String payload) {
    }

    private static final class Result {
        public static final int SUCCESS = 1000;
        public static final int INVALID_TOKEN = 2000;
        public static final int UNKNOWN_ERROR = 4000;
        public static final int INVALID_PREFIX = 6000;
        public static final int NETWORK_ERROR = 7000;
        public static final int INVALID_PAYLOAD = 8000;
    }

    private static final String RG_PATTERN = "rg%d|%d";
    private static final String DT_PATTERN = "dt%s";

    //这些属性是和这个websocket强关联的
    private boolean onlyLocal = true;
    private String clientId;
    private String email;
    private long userId;
    private final byte[] hsKey;
    //不能设置为static，因为每个函数需要控制内部的属性
    private final Map<String, Function<Param, Integer>> STRATEGY_MAP;
    private final Queue<Function<WebSocketSession, Integer>> AFTER_SUCCESS = new LinkedList<>();
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    FitbitWebApiService fitbitWebApiService;

    public LongSyncController(@Value("${jwt.hs-key:hello-world}") String strHsKey) {
        hsKey = strHsKey.getBytes(StandardCharsets.UTF_8);
        //由于采用一问一答的方式进行同步数据，所以这里就没有必要异步IO
        //因为就算你异步了，客户端也是在等你
        STRATEGY_MAP = Map.of(
                "tk", param -> {
                    Pair<JwtThreadSafe.Status, Map<String, Object>> statusMapPair = JwtThreadSafe.validateJwtTokenHMac(param.payload, hsKey);
                    if (statusMapPair.getA() != JwtThreadSafe.Status.SUCCESS) {
                        return Result.INVALID_TOKEN;
                    }
                    email = (String) statusMapPair.getB().get("email");
                    userId = Long.parseLong((statusMapPair.getB().get("userId")).toString());
                    try {
                        //检查是否有activate的手表，如果有，就同步从createTime到昨天
                        jdbcTemplate.query("""
                                        SELECT `clientId`,`createTime` FROM `watchTable`\s
                                        WHERE `userId`=? AND `expireTime`>? AND `activate`=1;
                                        """, new BeanPropertyRowMapper<>(Watch.class), userId, System.currentTimeMillis())
                                .stream().findFirst()
                                .ifPresentOrElse(
                                        //有正在工作的手表
                                        value -> {
                                            onlyLocal = false;
                                            clientId = value.getClientId();
                                            AFTER_SUCCESS.add(session -> {
                                                try {
                                                    session.sendMessage(new TextMessage(
                                                            String.format(RG_PATTERN,
                                                                    value.getCreateTime().toLocalDate().toEpochDay(),
                                                                    LocalDate.now().toEpochDay() - 1)
                                                    ));
                                                } catch (Exception e) {
                                                    return Result.NETWORK_ERROR;
                                                }
                                                return Result.SUCCESS;
                                            });
                                        },
                                        //没有正在工作的手表，返回本地数据的范围
                                        () -> AFTER_SUCCESS.add(session -> {
                                                    try {
                                                        List<Long> epochList = jdbcTemplate.query("""
                                                                (SELECT `epochDay` FROM `recordTable` WHERE `userId`=? ORDER BY `epochDay` LIMIT 1)\s
                                                                UNION ALL\s
                                                                (SELECT `epochDay` FROM `recordTable` WHERE `userId`=? ORDER BY `epochDay` DESC LIMIT 1)\s
                                                                ORDER BY `epochDay`;
                                                                """, new SingleColumnRowMapper<>(Long.class), userId, userId);
                                                        if (epochList.isEmpty()) {
                                                            session.sendMessage(new TextMessage(String.format(RG_PATTERN, 0, -1)));
                                                        } else if (epochList.size() == 1) {
                                                            session.sendMessage(new TextMessage(String.format(RG_PATTERN, epochList.get(0), epochList.get(0))));
                                                        } else {
                                                            session.sendMessage(new TextMessage(String.format(RG_PATTERN, epochList.get(0), epochList.get(1))));
                                                        }
                                                    } catch (Exception e) {
                                                        return Result.NETWORK_ERROR;
                                                    }
                                                    return Result.SUCCESS;
                                                }
                                        )
                                );
                    } catch (Exception e) {
                        return Result.UNKNOWN_ERROR;
                    }
                    return Result.SUCCESS;
                },
                "mr", param -> {
                    try {
                        String[] range = param.payload.split("\\|");
                        if (range.length != 2) {
                            return Result.INVALID_PAYLOAD;
                        }
                        long from = Long.parseLong(range[0]);
                        long to = Long.parseLong(range[1]);
                        log.info(String.format("email:%s 进行数据同步，接收mr请求，同步范围是：%d - %d", email, from, to));
                        if (to < from) {//返回空数据
                            AFTER_SUCCESS.add(session -> {
                                try {
                                    session.sendMessage(new TextMessage(String.format(
                                            DT_PATTERN, OBJECT_MAPPER.writeValueAsString(new LongSyncResp(Collections.emptyList()))
                                    )));
                                } catch (Exception e) {
                                    return Result.NETWORK_ERROR;
                                }
                                return Result.SUCCESS;
                            });
                            return Result.SUCCESS;
                        }
                        //最多30天的数据，正常的客户端不会请求超过这个数字
                        //如果超过了一定是故意的，直接无视
                        if (to - from > 29) {
                            to = from + 29;
                        }
                        List<Record> recordList = jdbcTemplate.query("""
                                SELECT * FROM `recordTable` WHERE `userId`=? AND `epochDay` BETWEEN ? AND ? ORDER BY `epochDay`;
                                """, new BeanPropertyRowMapper<>(Record.class), userId, from, to);
                        List<DayRecord> dayRecords = new ArrayList<>(recordList.stream()
                                .collect(Collectors.groupingBy(
                                        Record::getEpochDay,
                                        LinkedHashMap::new,//保证dayRecords按照epochDay升序
                                        Collectors.collectingAndThen(
                                                Collectors.toList(),
                                                list -> {
                                                    DayRecord dr = new DayRecord();
                                                    dr.getRecordList().addAll(list);
                                                    return dr;
                                                }
                                        )
                                ))
                                .values());
                        if (onlyLocal) {//前面的查询认为现在没有可用的手表
                            log.info(String.format("email:%s 进行数据同步，采用local模式", email));
                            AFTER_SUCCESS.add(session -> {
                                try {
                                    session.sendMessage(new TextMessage(String.format(
                                            DT_PATTERN, OBJECT_MAPPER.writeValueAsString(new LongSyncResp(dayRecords))
                                    )));
                                } catch (Exception e) {
                                    log.info(String.format("email:%s 进行数据同步，采用local模式，数据发送失败", email));
                                    return Result.UNKNOWN_ERROR;
                                }
                                log.info(String.format("email:%s 进行数据同步，采用local模式，数据成功发送", email));
                                return Result.SUCCESS;
                            });
                            return Result.SUCCESS;
                        }
                        //如果有可用的手表
                        //看看本地缺少了哪些数据，把哪些数据查出来，先查本地数据，然后网络查询数据，最后汇总返回给客户端
                        log.info(String.format("email:%s 进行数据同步，采用local-network模式", email));
                        List<Long> lackData = getLackData(dayRecords, from, to);
                        //根据lackData进行网络请求拿到数据
                        if (lackData.size() == 1) {
                            fitbitWebApiService.getRangeRecordAndAsyncToDatabase(
                                    userId, clientId, lackData.get(0), lackData.get(0)
                            ).ifPresent(dayRecords::addAll);
                        } else if (lackData.size() > 1) {
                            //尽可能进行批量查询
                            int lastIndex = 0;
                            for (int i = 1; i < lackData.size(); ++i) {
                                long e1 = lackData.get(i - 1);
                                long e2 = lackData.get(i);
                                if (e1 + 1 != e2) {
                                    fitbitWebApiService.getRangeRecordAndAsyncToDatabase(
                                            userId, clientId, lackData.get(lastIndex), e1
                                    ).ifPresent(dayRecords::addAll);
                                    lastIndex = i;
                                }
                                if (i == lackData.size() - 1) {//最后一批
                                    fitbitWebApiService.getRangeRecordAndAsyncToDatabase(
                                            userId, clientId, lackData.get(lastIndex), e2
                                    ).ifPresent(dayRecords::addAll);
                                    break;
                                }
                            }
                        }
                        log.info(String.format("email:%s 进行数据同步，采用local-network模式, 请求数据结束",email));
                        AFTER_SUCCESS.add(session -> {
                            try {
                                session.sendMessage(new TextMessage(String.format(
                                        DT_PATTERN, OBJECT_MAPPER.writeValueAsString(new LongSyncResp(dayRecords))
                                )));
                            } catch (Exception e) {
                                log.info(String.format("email:%s 进行数据同步，采用local-network模式，数据发送失败%s", email,e.getCause()));
                                return Result.NETWORK_ERROR;
                            }
                            log.info(String.format("email:%s 进行数据同步，采用local-network模式，数据成功发送", email));
                            return Result.SUCCESS;
                        });
                        return Result.SUCCESS;
                    } catch (Exception e) {
                        log.info(String.format("email:%s 进行数据同步，出现未知错误%s", email, Arrays.toString(e.getStackTrace())));
                        return Result.UNKNOWN_ERROR;
                    }
                }
        );
    }

    @NotNull
    private static List<Long> getLackData(List<DayRecord> dayRecords, long from, long to) {
        List<Long> lackData = new ArrayList<>();
        int size = dayRecords.size();
        int j = 0;
        for (long i = from; i <= to; ) {
            if (j >= size) {
                lackData.add(i);
                ++i;
            } else {
                long e = dayRecords.get(j).getRecordList().get(0).epochDay;
                if (i < e) {
                    lackData.add(i);
                    ++i;
                } else if (i > e) {
                    ++j;
                } else {
                    ++i;
                }
            }
        }
        return lackData;
    }

    /**
     * 交互流程：
     * 客户端->服务端 tk token tkABC.DEF.GHI
     * 服务端->客户端 rg range rg%d|%d
     * 客户端->服务端 mr my range mr%d|%d
     * 服务端->客户端 dt data dt{json}
     * 客户端接收完数据，关闭连接
     */
    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, @NotNull TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        @NotNull String text = message.getPayload();
        if (text.length() <= 2 || !STRATEGY_MAP.containsKey(text.substring(0, 2))) {
            session.close(new CloseStatus(Result.INVALID_PREFIX));
            return;
        }
        final String prefix = text.substring(0, 2);
        final String payload = text.substring(2);
        Param param = new Param(session, payload);
        int result = STRATEGY_MAP.get(prefix).apply(param);
        if (result != Result.SUCCESS) {
            session.close(new CloseStatus(result));
            return;
        }
        @Nullable Function<WebSocketSession, Integer> poll = AFTER_SUCCESS.poll();
        if (poll != null) {
            result = poll.apply(session);
            if (result != Result.SUCCESS) {
                session.close(new CloseStatus(result));
            }
        }
    }

    //心跳包
    @Override
    protected void handlePongMessage(@NotNull WebSocketSession session, @NotNull PongMessage message) throws Exception {
        super.handlePongMessage(session, message);
        //暂时什么都不做
    }

    @Override
    public void handleTransportError(@NotNull WebSocketSession session, @NotNull Throwable exception) throws Exception {
        super.handleTransportError(session, exception);
        log.error(String.format("email为%s 的数据同步出现异常", email));
        session.close(new CloseStatus(Result.UNKNOWN_ERROR));
    }
}