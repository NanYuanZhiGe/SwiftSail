package com.nyzg.user.dbobj;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TrustDevice {
    long id;
    String email;
    String deviceName;
    String deviceId;
}