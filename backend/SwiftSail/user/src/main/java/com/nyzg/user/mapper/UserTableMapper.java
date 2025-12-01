package com.nyzg.user.mapper;

import com.nyzg.user.dbobj.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserTableMapper {
    int isEmailExists(String email);
    int isNickNameExists(String nickName);

    int isHaveEmailAndSecretWord(@Param("email") String email, @Param("secretWord") byte[] secretWord);

    int isHaveEmailAndSecretKey(String mail);

    List<User> getUserByEmail(@Param("email")String email);

    Long getUserIdByEmail(@Param("email")String email);

    List<User> getSecretPubKey(String mail);
    void insertUserIfNotExists(User user);
}
