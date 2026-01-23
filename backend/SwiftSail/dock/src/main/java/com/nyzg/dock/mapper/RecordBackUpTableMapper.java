package com.nyzg.dock.mapper;

import com.nyzg.dock.netobj.RecordBackUpAndroid;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecordBackUpTableMapper {
    void insertRecordBackUpAndroid(@Param("androidDataList") List<RecordBackUpAndroid> androidDataList);
}
