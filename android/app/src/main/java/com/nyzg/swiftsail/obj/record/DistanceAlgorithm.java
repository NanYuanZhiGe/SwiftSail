package com.nyzg.swiftsail.obj.record;

import android.location.Location;

import androidx.annotation.NonNull;

public abstract class DistanceAlgorithm {
    private boolean hasInit = false;
    private double refLat;
    private double refLon;

    public abstract double update(double gx, double gy);

    public abstract double summary();

    public void reset() {
        reset0();
        hasInit = false;
    }

    protected abstract void reset0();

    public double update(@NonNull double[] pair) {
        return update(pair[0], pair[1]);
    }

    /**
     * @return 返回变化值
     */
    public double update(@NonNull Location location) {
        if (!hasInit) {
            refLat = location.getLatitude();
            refLon = location.getLongitude();
            hasInit = true;
        }
        return update(gpsLocationToPlatformLocation(location.getLatitude(), location.getLongitude(), refLat, refLon));
    }

    public static double[] gpsLocationToPlatformLocation(double gLat, double gLon, double refLat, double refLon) {
        double[] pair = new double[]{gLat, gLon};
        final double R = 6378137; // 地球半径（米）
        double dLat = Math.toRadians(gLat - refLat);
        double dLon = Math.toRadians(gLon - refLon);
        double refLatRad = Math.toRadians(refLat);

        double x = dLon * R * Math.cos(refLatRad);
        double y = dLat * R;
        pair[0] = x;
        pair[1] = y;
        return pair;
    }
}
