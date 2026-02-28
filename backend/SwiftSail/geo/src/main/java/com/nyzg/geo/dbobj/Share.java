package com.nyzg.geo.dbobj;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Share {
    long userId;
    String k;
    String v;
}
