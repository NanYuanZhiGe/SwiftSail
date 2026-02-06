package com.nyzg.dock.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

@Mapper
public interface UserTableMapper {
    LocalDateTime getCreateTime(long userId);
}
