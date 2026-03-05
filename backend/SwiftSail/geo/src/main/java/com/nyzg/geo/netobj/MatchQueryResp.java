package com.nyzg.geo.netobj;

import java.util.List;

public class MatchQueryResp {
    public List<ReleasedMatch> releasedMatchList;

    public MatchQueryResp(){
    }

    public MatchQueryResp(List<ReleasedMatch> releasedMatchList) {
        this.releasedMatchList = releasedMatchList;
    }
}
