package com.nyzg.dock.netobj;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class WatchSyncCheckResp {
    long userId;
    Map<String,Boolean> resultMap;
}
