package com.nyzg.dock.mapper;

import com.nyzg.dock.dbobj.Watch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WatchTableMapper {
    void insertWatch(Watch watch);

    /**
     * watch里面就隐含了userId，所以不用额外传参
     */
    void updateWatch(@Param("watch") Watch watch, @Param("clientId") String clientId);

    void updateWatchToken(
            @Param("clientId") String clientId,
            @Param("accessToken") String accessToken,
            @Param("refreshToken") String refreshToken,
            @Param("watchUserId") String watchUserId,
            @Param("expireTime") long expireTime
    );

    /**
     * 把对应的手表设置为下线模式
     */
    void updateWatchOffline(
            @Param("userId") long userId,
            @Param("clientId") String clientId
    );

    String getWatchUserId(@Param("userId") long userId, @Param("clientId") String clientId);

    List<Watch> checkSyncStatus(@Param("userId") long userId, @Param("clientIdList") List<String> clientIdList);

    void deleteWatch(@Param("userId") long userId, @Param("clientId") String clientId);

    List<Watch> selectWatchByClientId(@Param("userId") long userId, @Param("clientId") String clientId);
    List<Watch> selectWatchByUserId(@Param("userId")long userId);

    /**
     * 返回过期时间和authorizeHeader，以及refreshToken
     */
    Watch checkExpireTime(@Param("clientId") String clientId);

    void updateRefreshToken(
            @Param("clientId") String clientId,
            @Param("accessToken") String accessToken,
            @Param("refreshToken") String refreshToken,
            @Param("expireTime") long expireTime
    );

    /**
     * @return 返回accessToken和userId，以及expireTime
     */
    Watch getAccessToken(@Param("clientId") String clientId);

    /**
     * 把指定手表的id的过期时间设置为0L
     */
    void markWatchExpire(@Param("clientId") String clientId);
}
