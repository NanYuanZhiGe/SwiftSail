package com.nyzg.geo.netobj;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class MatchUserResp {
    List<Long> userId;

    public MatchUserResp(List<Long> userId) {
        this.userId = userId;
    }
}
