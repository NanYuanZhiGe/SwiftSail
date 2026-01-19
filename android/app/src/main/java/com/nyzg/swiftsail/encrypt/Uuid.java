package com.nyzg.swiftsail.encrypt;

import java.util.UUID;

public class Uuid {
    public static String getUuidString36() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
}
