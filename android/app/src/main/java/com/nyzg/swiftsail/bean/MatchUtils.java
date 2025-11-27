package com.nyzg.swiftsail.bean;

import java.util.regex.Pattern;

public class MatchUtils {
    static private final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    public static boolean isMailAddrIllegal(String mailAddr) {
        if (mailAddr == null) {
            return true;
        }
        return !EMAIL_PATTERN.matcher(mailAddr).matches();
    }
}
