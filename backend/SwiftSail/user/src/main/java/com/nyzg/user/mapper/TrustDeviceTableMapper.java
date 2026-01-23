package com.nyzg.user.mapper;

import com.nyzg.user.dbobj.TrustDevice;
import org.apache.ibatis.annotations.Param;

public interface TrustDeviceTableMapper {
    int isUserDeviceInTable(@Param("email") String email, @Param("deviceId") String deviceId);
    void insertDeviceInTable(TrustDevice trustDevice);
}
