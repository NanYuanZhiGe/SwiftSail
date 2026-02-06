package com.nyzg.dock.mapper;

import com.nyzg.dock.dbobj.Record;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecordTableMapper {
    void insertRecordIntoTable(Record record);

    void insertRecordListIntoTable(@Param("recordList")List<Record> recordList);

    List<Record> selectRecordFromAndEnd(
            @Param("userId") long userId,
            @Param("fromDay") long fromDay,
            @Param("endDay") long endDay
    );
}
