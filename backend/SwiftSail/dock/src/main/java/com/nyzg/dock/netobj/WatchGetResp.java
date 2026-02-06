package com.nyzg.dock.netobj;

import com.nyzg.dock.dbobj.Watch;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class WatchGetResp {
    List<Watch> watchList;

    public WatchGetResp(List<Watch> watchList) {
        this.watchList = watchList;
    }
}
