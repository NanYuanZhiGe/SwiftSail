package com.nyzg.swiftsail.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.nyzg.swiftsail.dbobj.User;

import java.util.List;

@Dao
public interface UserTable {
    @Insert
    void insertUser(User user);

    @Query("SELECT * FROM userTable;")
    List<User> selectAllUser();
}
