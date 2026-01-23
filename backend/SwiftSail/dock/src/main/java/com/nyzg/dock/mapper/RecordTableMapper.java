package com.nyzg.dock.mapper;

import com.nyzg.dock.dbobj.Record;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RecordTableMapper {
    void insertRecordIntoTable(Record record);
}
