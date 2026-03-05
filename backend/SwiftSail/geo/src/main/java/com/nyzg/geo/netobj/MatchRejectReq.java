package com.nyzg.geo.netobj;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MatchRejectReq {
    String matchId;
    long other;
}
