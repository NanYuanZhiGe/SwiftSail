package com.nyzg.dock.mapper;

import com.nyzg.dock.dbobj.Watch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WatchTableMapper {
    void insertWatch(Watch watch);

    void updateWatchToken(
            @Param("clientId") String clientId,
            @Param("accessToken") String accessToken,
            @Param("refreshToken") String refreshToken,
            @Param("watchUserId") String watchUserId,
            @Param("expireTime") long expireTime
    );

    List<Watch> selectWatchByClientId(@Param("clientId") String clientId);

    /**
     * 返回过期时间和authorizeHeader
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
