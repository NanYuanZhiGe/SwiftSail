package com.nyzg.geo.netobj;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class MatchQueryRelatedUserResp {
    //matchId
    List<User> userList;

    public MatchQueryRelatedUserResp(List<User> userList) {
        this.userList = userList;
    }

    @Data
    @NoArgsConstructor
    public static class User {
        String matchId;
        long userId;
        String name;
        String appellation;
        String headIcon;
    }
}
