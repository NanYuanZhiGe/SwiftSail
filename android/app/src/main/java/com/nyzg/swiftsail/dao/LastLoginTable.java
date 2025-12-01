package com.nyzg.swiftsail.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.nyzg.swiftsail.dbobj.LastLogin;

import java.util.List;

@Dao
public interface LastLoginTable {
    @Query("SELECT * FROM lastLoginTable;")
    List<LastLogin> selectAllFromLastLoginTable();

    @Query("DELETE FROM `lastLoginTable`;")
    void deleteAll();

    @Insert
    void insertLastLogin(LastLogin lastLogin);
}
