package com.nyzg.geo.controller;

import com.nyzg.common.DateUtils;
import com.nyzg.common.netobj.BaseResp;
import com.nyzg.common.netobj.HttpResp;
import com.nyzg.geo.netobj.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class MatchController {
    @Resource
    JdbcTemplate jdbcTemplate;

    @Resource
    PlatformTransactionManager transactionManager;

    public final static HttpResp NO_MATCH = new HttpResp(false, "匹配失败，该匹配可能被抢先一步或对方取消");
    public final static HttpResp MATCH_REJECT = new HttpResp(false, "您被改匹配拒绝，无法加入");

    private HttpResp validateId(String userId) {
        long uId;
        try {
            uId = Long.parseLong(userId);
        } catch (Exception ignore) {
            return BaseResp.LOGIN_REQUIRED;
        }
        return null;
    }

    @PostMapping("/match/query/user")
    public HttpResp matchQueryUser(
            @RequestHeader("userId") String userId,
            @RequestBody String matchId) {
        long uId;
        try {
            uId = Long.parseLong(userId);
        } catch (Exception ignore) {
            return BaseResp.LOGIN_REQUIRED;
        }
        try {
            //用户必须是发布过这个东西的
            Integer count = jdbcTemplate.query("""
                    SELECT COUNT(1) as count FROM `releasedMatchTable` WHERE `userId`=? AND `matchId`=?
                    """, (rs, num) -> rs.getInt("count"), uId, matchId).stream().findFirst().orElse(null);
            if (count == null || count == 0) {
                return new HttpResp(true, new MatchUserResp(Collections.emptyList()));
            }
            return new HttpResp(true, new MatchUserResp(jdbcTemplate.query("""
                    SELECT `userId` FROM participateTable WHERE matchId=? AND userId!=? AND status>0  \s
                    """, (rs, num) -> rs.getLong("userId"), matchId, uId)));
        } catch (Exception e) {
            log.error("/match/query/user", e);
            return HttpResp.COMMON_ERROR;
        }
    }

    @GetMapping("/match/query/related/user")
    public HttpResp matchQueryRelatedUser(@RequestHeader("userId") String userId) {
        {
            HttpResp resp = validateId(userId);
            if (resp != null) {
                return resp;
            }
        }
        try {
            long uId = Long.parseLong(userId);
            return new HttpResp(true, HttpResp.COMMON_SUCCESS_CODE, "", new TransactionTemplate(transactionManager)
                    .execute(status -> {
                        List<String> matchIdList = jdbcTemplate.query("""
                                SELECT `matchId` FROM `releasedMatchTable` WHERE `userId`=?
                                """, (rs, num) -> rs.getString("matchId"), uId);
                        if (matchIdList.isEmpty()) {
                            return new MatchQueryRelatedUserResp(Collections.emptyList());
                        }
                        String inSql = matchIdList.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(", "));

                        String sql = """
                                SELECT\s
                                p.matchId as matchId,
                                p.userId as userId,
                                u.nickName as nickName,
                                pi.appellation as appellation,
                                s.v AS headIconUrl
                                FROM participateTable p
                                JOIN userTable u ON p.userId = u.id
                                LEFT JOIN `personalDataTable` pi ON p.userId = pi.userId
                                LEFT JOIN shareTable s ON p.userId = s.userId AND s.k = 'headIconUrl'
                                WHERE p.matchId IN (
                                """ + inSql + ")";

                        Object[] args = matchIdList.toArray();
                        return new MatchQueryRelatedUserResp(jdbcTemplate.query(
                                sql,
                                (rs, num) -> {
                                    MatchQueryRelatedUserResp.User user = new MatchQueryRelatedUserResp.User();
                                    user.setMatchId(rs.getString("matchId"));
                                    user.setUserId(rs.getLong("userId"));
                                    user.setName(rs.getString("nickName"));
                                    user.setAppellation(rs.getString("appellation"));
                                    user.setHeadIcon(rs.getString("headIconUrl"));
                                    return user;
                                }, args));
                    }));
        } catch (Exception e) {
            log.error("/match/query/related/user", e);
            return BaseResp.INTERNAL_ERROR;
        }
    }

    @GetMapping("/match/query")
    public HttpResp matchQuery(@RequestHeader("userId") String userId) {
        long uId;
        try {
            uId = Long.parseLong(userId);
        } catch (Exception ignore) {
            return BaseResp.LOGIN_REQUIRED;
        }
        try {
            List<String> matchIdList = jdbcTemplate.query("""
                    SELECT `matchId` FROM `participateTable` WHERE `userId`=? AND `status`>0;
                    """, (rs, num) -> rs.getString("matchId"), uId);
            if (matchIdList.isEmpty()) {
                return new HttpResp(true, new MatchQueryResp(Collections.emptyList()));
            }
            String placeholders = matchIdList.stream()
                    .map(id -> "?")
                    .collect(Collectors.joining(", "));

            String sql = """
                    SELECT `userId`,`spot`,
                    ST_Y(spotDetail) AS lat,ST_X(spotDetail) AS lon,
                    `startTime`,`hour`,`minute`,`cutTime`,`sport`,
                    `part`,`myLevel`,`matchLevel1`,`matchLevel2`,
                    `matchDesc`,`feeType`,`fee`,`status`,`userCount`,
                    `matchId`
                    FROM `releasedMatchTable` WHERE `matchId` IN (
                    """ + placeholders + ")";
            return new HttpResp(true, new MatchQueryResp(jdbcTemplate.query(sql, (rs, num) -> {
                ReleasedMatch match = new ReleasedMatch();
                match.userId = rs.getLong("userId");
                match.spot = rs.getString("spot");
                match.lat = rs.getDouble("lat") + "";
                match.lon = rs.getDouble("lon") + "";
                match.startTime = rs.getString("startTime");
                match.hour = rs.getShort("hour");
                match.minute = rs.getShort("minute");
                match.cutTime = rs.getShort("cutTime");
                match.sport = rs.getShort("sport");
                match.part = rs.getShort("part");
                match.myLevel = rs.getShort("myLevel");
                match.matchLevel1 = rs.getShort("matchLevel1");
                match.matchLevel2 = rs.getShort("matchLevel2");
                match.matchDesc = rs.getString("matchDesc");
                match.feeType = rs.getShort("feeType");
                match.fee = rs.getBigDecimal("fee").toString();
                match.status = rs.getShort("status");
                match.userCount = rs.getShort("userCount");
                match.matchId = rs.getString("matchId");
                return match;
            }, matchIdList.toArray())));
        } catch (Exception e) {
            log.error("/match/query", e);
            return HttpResp.COMMON_ERROR;
        }
    }

    @PostMapping("/match/quit")
    public HttpResp matchQuit(
            @RequestHeader("userId") String userId,
            @RequestBody String matchId) {
        long uId;
        try {
            uId = Long.parseLong(userId);
        } catch (Exception ignore) {
            return BaseResp.LOGIN_REQUIRED;
        }
        try {
            return new TransactionTemplate(transactionManager)
                    .execute(status -> {
                        //如果检查用户是否有这个表
                        //同时for update，锁住这个这一个数据
                        Long user = jdbcTemplate.query("""
                                        SELECT `userId` FROM `releasedMatchTable` WHERE `userId`=? AND `matchId`=? FOR UPDATE
                                        """, (rs, num) -> rs.getLong("userId")).stream()
                                .findFirst().orElse(null);
                        if (user == null) {
                            return HttpResp.COMMON_SUCCESS;
                        }
                        //如果拥有整个表，就删除所有更新状态
                        if (user == uId) {
                            jdbcTemplate.update("""
                                    UPDATE `releasedMatchTable` SET `status`=-1 WHERE `userId`=? AND `matchId`=?
                                    """, uId, matchId);
                        } else {//如果用户支持参与者，就更新两个表
                            jdbcTemplate.update("""
                                    DELETE FROM `participateTable` WHERE `userId`=? AND `matchId`=?
                                    """, uId, matchId);
                            jdbcTemplate.update("""
                                    UPDATE `releasedMatchTable` SET userCount=userCount-1 WHERE `matchId`=?;
                                    """, matchId);
                        }
                        return HttpResp.COMMON_SUCCESS;
                    });
        } catch (Exception e) {
            log.error("/match/quit", e);
            return HttpResp.COMMON_ERROR;
        }
    }

    @PostMapping("/match/reject")
    public HttpResp matchReject(
            @RequestHeader("userId") String userId,
            @RequestBody MatchRejectReq req) {
        long uId;
        try {
            uId = Long.parseLong(userId);
        } catch (Exception ignore) {
            return BaseResp.LOGIN_REQUIRED;
        }
        if (req == null
                || req.getMatchId() == null) {
            return HttpResp.COMMON_SUCCESS;
        }
        try {
            return new TransactionTemplate(transactionManager).execute(transactionStatus -> {
                //确定用户拥有这张表
                //且这张表确实在匹配
                int rows = jdbcTemplate.update("""
                        UPDATE `releasedMatchTable` SET userCount=userCount-1 WHERE userId=? AND matchId=? AND userCount>1;
                        """, uId, req.getMatchId());
                if (rows < 1) {
                    return HttpResp.COMMON_SUCCESS;
                }
                //更行表
                jdbcTemplate.update("""
                        UPDATE `participateTable` SET status=0 WHERE matchId=? AND userId=?
                        """, req.getMatchId(), req.getOther());
                return HttpResp.COMMON_SUCCESS;
            });
        } catch (Exception e) {
            log.error("/match/reject/");
            return HttpResp.COMMON_ERROR;
        }
    }

    @PostMapping("/match/start")
    public HttpResp matchStart(
            @RequestHeader("userId") String userId,
            @RequestBody String matchId) {
        long uId;
        try {
            uId = Long.parseLong(userId);
        } catch (Exception ignore) {
            return BaseResp.LOGIN_REQUIRED;
        }
        if (matchId == null || matchId.isBlank()) {
            return HttpResp.COMMON_SUCCESS;
        }
        try {
            jdbcTemplate.update("""
                    UPDATE `releasedMatchTable` SET `status`=1 WHERE matchId=? AND userId=? AND status=2
                    """, matchId, uId);
            return HttpResp.COMMON_SUCCESS;
        } catch (Exception e) {
            log.error("/match/start", e);
            return HttpResp.COMMON_ERROR;
        }
    }

    @PostMapping("/match/participate")
    public HttpResp matchParticipate(
            @RequestHeader("userId") String userId,
            @RequestBody String matchId) {
        long uId;
        try {
            uId = Long.parseLong(userId);
        } catch (Exception ignore) {
            return BaseResp.LOGIN_REQUIRED;
        }
        if (matchId == null || matchId.isBlank()) {
            return BaseResp.WRONG_PARAM;
        }
        try {
            return new TransactionTemplate(transactionManager)
                    .execute(transactionStatus -> {
                        //是否被拒绝
                        short allow = jdbcTemplate.query("""
                                        SELECT `status` FROM `participateTable` WHERE `matchId`=? AND `userId`=?
                                        """, (rs, num) -> rs.getShort("status"), matchId, uId)
                                .stream().findFirst().orElse((short) 3);//查不到就是3
                        if (allow == 0) {
                            return MATCH_REJECT;
                        }
                        if (allow != 3) {//已经加入了
                            return HttpResp.COMMON_SUCCESS;
                        }
                        //for update，强制加锁
                        //要求当前人数小于可匹配的人数，然后是可匹配的状态
                        //当前的时间
                        long time = LocalDateTime.now().toEpochSecond(DateUtils.ZONE_OFFSET);
                        ReleasedMatch match = jdbcTemplate.query("""
                                SELECT `userCount` FROM `releasedMatchTable` WHERE matchId=? AND userCount<part AND status=2 AND startTimeCount>? FOR UPDATE;
                                """, (rs, num) -> {
                            ReleasedMatch releasedMatch = new ReleasedMatch();
                            releasedMatch.userCount = rs.getShort("userCount");
                            return releasedMatch;
                        }, matchId, time).stream().findFirst().orElse(null);
                        //查不到
                        if (match == null) {
                            return NO_MATCH;
                        }
                        //如果查到，更新两个表
                        if (match.userCount == 1) {//更新match的状态为进行中
                            jdbcTemplate.update("""
                                    UPDATE `releasedMatchTable` SET userCount=? WHERE matchId=?
                                    """, match.userCount + 1, matchId);
                        } else {
                            jdbcTemplate.update("""
                                    UPDATE `releasedMatchTable` SET userCount=? WHERE matchId=?
                                    """, match.userCount + 1, matchId);
                        }
                        //插入参与表，状态默认是1-可匹配
                        jdbcTemplate.update("""
                                INSERT INTO `participateTable` (matchId, userId) VALUES (?,?)
                                """, matchId, uId);
                        return HttpResp.COMMON_SUCCESS;
                    });
        } catch (Exception e) {
            log.error("/match/participate", e);
            return HttpResp.COMMON_ERROR;
        }
    }

    @PostMapping("/match/post")
    public HttpResp matchPost(
            @RequestHeader("userId") String userId,
            @RequestBody ReleasedMatch releasedMatch) {
        //参数校验
        long uId;
        try {
            uId = Long.parseLong(userId);
        } catch (Exception ignore) {
            return BaseResp.LOGIN_REQUIRED;
        }
        {
            String m = releasedMatch.validateSucceedAtNull();
            if (m != null) {
                return new HttpResp(false, m);
            }
        }
        //校验成功，就插入数据
        try {
            new TransactionTemplate(transactionManager)
                    .executeWithoutResult(transactionStatus -> {
                        //插入数据
                        String matchId = UUID.randomUUID().toString();
                        jdbcTemplate.update("""
                                        INSERT INTO `releasedMatchTable` (
                                        userId, spot, spotDetail,
                                        startTime, hour, minute, cutTime,
                                        sport, part, myLevel, matchLevel1,
                                        matchLevel2, matchDesc, feeType,
                                        fee,matchId,startTimeCount
                                        ) VALUES (
                                        ?,?,ST_PointFromText(?),
                                        ?,?,?,?,
                                        ?,?,?,?,
                                        ?,?,?,
                                        ?,?,
                                        ?
                                        )
                                        """,
                                uId, releasedMatch.spot, String.format("POINT(%s %s)", releasedMatch.lon, releasedMatch.lat),
                                releasedMatch.startTime, (short) releasedMatch.hour, (short) releasedMatch.minute, (short) releasedMatch.cutTime,
                                releasedMatch.sport, releasedMatch.part, releasedMatch.myLevel, releasedMatch.matchLevel1,
                                releasedMatch.matchLevel2, releasedMatch.matchDesc == null ? "" : releasedMatch.matchDesc, releasedMatch.feeType,
                                new BigDecimal(releasedMatch.fee), matchId,
                                LocalDateTime.parse(releasedMatch.startTime, ReleasedMatch.DATE_TIME_FORMATTER).toEpochSecond(DateUtils.ZONE_OFFSET)
                        );
                        //更新表
                        jdbcTemplate.update("""
                                INSERT INTO `participateTable` (matchId, userId) VALUES (?,?)
                                """, matchId, uId);
                    });
        } catch (Exception e) {
            log.error("添加match", e);
            return HttpResp.COMMON_ERROR;
        }
        return HttpResp.COMMON_SUCCESS;
    }
}
