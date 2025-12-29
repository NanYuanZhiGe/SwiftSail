package com.nyzg.swiftsail.encrypt;

import java.util.Optional;
import java.util.UUID;

public class Uuid {
    public static byte[] getUuidBytes() {
        UUID uuid = UUID.randomUUID();
        return uuidToBytes(uuid);
    }

    static public byte[] uuidToBytes(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        byte[] buffer = new byte[16];
        for (int i = 0; i < 8; i++) {
            buffer[i] = (byte) (msb >>> (8 * (7 - i)));
        }
        for (int i = 0; i < 8; i++) {
            buffer[i + 8] = (byte) (lsb >>> (8 * (7 - i)));
        }
        return buffer;
    }

    public static Optional<String> bytesToString(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return Optional.empty();
        }
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (bytes[i] & 0xFF);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (bytes[i] & 0xFF);
        }
        return Optional.of(new UUID(msb, lsb).toString());
    }

    public static Optional<byte[]> stringToBytes(String s) {
        try {
            return Optional.of(uuidToBytes(UUID.fromString(s)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
