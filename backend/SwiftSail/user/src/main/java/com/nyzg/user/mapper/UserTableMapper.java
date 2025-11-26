package com.nyzg.user.mapper;

import com.nyzg.user.dbobj.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserTableMapper {
    int isEmailExists(String email);
    int isNickNameExists(String nickName);

    int isHaveEmailAndSecretWord(String email,byte[] secretWord);

    int isHaveEmailAndSecretKey(String mail);

    List<User> getSecretPubKey(String mail);
    void insertUserIfNotExists(User user);
}
