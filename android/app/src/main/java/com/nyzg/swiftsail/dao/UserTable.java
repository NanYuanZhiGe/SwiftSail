package com.nyzg.swiftsail.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.nyzg.swiftsail.dbobj.User;

import java.util.List;

@Dao
public interface UserTable {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(User user);

    @Query("SELECT * FROM userTable;")
    List<User> selectAllUser();

    @Query("SELECT * FROM `userTable` WHERE `id`=:id;")
    User selectSingleUser(long id);

    @Query("SELECT `token` FROM `userTable` WHERE `id`=:id;")
    String selectTokenFromUserTable(long id);

    @Query("SELECT `id` FROM `userTable` WHERE `email`=:email;")
    Long selectUserIdByEmail(String email);

    @Query("UPDATE `userTable` SET `token`=:token WHERE `id`=:id;")
    void updateUserLoginToken(long id, String token);

}
