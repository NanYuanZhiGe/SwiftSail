package com.nyzg.distance_count.kf;

import android.location.Location;

import androidx.annotation.NonNull;

public abstract class KF {
    private final static double EARTH_RADIUS = 6371000;//地球半径

    abstract public double getDistance(Location location);

    /**
     * 转换公式：
     * 假设标准点为：P0
     * x=R*(longitude-P0.longitude)*cos(P0.latitude)
     * y=R*(latitude-P0.latitude)
     *
     * @param basePoint 参考点
     * @param latitude  维度
     * @param longitude 经度
     * @return 以参考点为坐标原点，根据经纬度转换得到的坐标点
     */
    final protected Point transformPoint2Platform(@NonNull EarthPoint basePoint, double latitude, double longitude) {
        double latRadius = Math.toRadians(latitude);
        double longRadius = Math.toRadians(longitude);

        double x = EARTH_RADIUS * (longRadius - basePoint.longRadius) * Math.cos(basePoint.latRadius);
        double y = EARTH_RADIUS * (latRadius - basePoint.latRadius);
        Point res = new Point();
        res.x = x;
        res.y = y;
        return res;
    }

    /**
     * 把经纬度坐标转换为弧度制
     */
    final protected EarthPoint transformToEarthPoint(double latitude, double longitude) {
        EarthPoint earthPoint = new EarthPoint();
        earthPoint.latRadius = Math.toRadians(latitude);
        earthPoint.longRadius = Math.toRadians(longitude);
        return earthPoint;
    }

    protected static final class EarthPoint {
        public double latRadius;//维度弧度
        public double longRadius;//经度弧度
    }

    protected static final class Point {
        public double x;
        public double y;
    }
}
