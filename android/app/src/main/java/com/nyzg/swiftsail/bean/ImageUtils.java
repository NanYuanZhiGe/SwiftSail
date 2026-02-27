package com.nyzg.swiftsail.bean;

import java.net.URI;

public class ImageUtils {
    public static String getHeadIconURI(long userId) {
        return ServerURL.BASE_IMAGE_URL + userId + ServerURL.HEAD_ICON_SUFFIX;
    }
}
