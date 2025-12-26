package com.nyzg.distance_count.kf;

import android.location.Location;

import org.ejml.simple.SimpleMatrix;

public class KfImp2D extends KF {
    private EarthPoint basePoint;
    private Point lastPoint;
    private double lastDistance = 0;
    private SimpleMatrix x = new SimpleMatrix(4, 1);//状态向量
    private SimpleMatrix P = new SimpleMatrix(4, 4);//协方差
    private final double POSITION_NOISE = 5;
    private final double ACCELERATE_NOISE = 1;
    private long lastMms = 0;

    public KfImp2D() {
        //初始化协方差
        P.set(0, 0, 1000);
        P.set(1, 1, 1000);
        P.set(2, 2, 100);
        P.set(3, 3, 100);
    }

    /**
     * 卡尔曼滤波计算过程：
     * 1. 状态向量 xt=[xt, yt, vxt, vyt]
     * 2. 状态转移方程 xt+1=Fxt
     * 4. 状态转移矩阵为F，噪声为Q，
     * 5. 观测方程 zt=Hxt+vt，其中观测方程为H
     * 6. 观测协方差为R，
     * 7. 卡尔曼预测：xt+1=Fxt, Pt+1=FPtF^T+Q
     * 8. 更新 yt=zt+1-H*xt|t-1, St=HPt|t-1H^T+R, Kt=Pt|t-1H^TSt^-1, xt=xt|t-1+Kt*yt, Pt=(I-KtH)Pt|t-1
     *
     * @param location 坐标，LocationManager获取的原始数据
     * @return 当前计算的总路程
     */
    @Override
    public double getDistance(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        //首次执行
        if (basePoint == null) {
            basePoint = this.transformToEarthPoint(latitude, longitude);
            lastMms = location.getTime() > 0 ? location.getTime() : System.currentTimeMillis();
            return 0;
        }
        //获取时间间隔，单位为秒
        double dt = .001 * (location.getTime() > 0 ? location.getTime() - lastMms : System.currentTimeMillis() - lastMms);
        //经纬度转化成为平面坐标系的坐标
        Point curPos = this.transformPoint2Platform(basePoint, latitude, longitude);

        //---------------KF begin----------------------
        SimpleMatrix F = new SimpleMatrix(4, 4, true,
                1, 0, dt, 0,
                0, 1, 0, dt,
                0, 0, 1, 0,
                0, 0, 0, 1
        );
        SimpleMatrix predictX = F.mult(this.x);
        double q = ACCELERATE_NOISE * ACCELERATE_NOISE;//噪声
        double dt2 = dt * dt;
        double dt3 = dt2 * dt;
        double dt4 = dt3 * dt;
        SimpleMatrix Q = new SimpleMatrix(4, 4, true,
                q * dt4 / 4, 0, q * dt3 / 2, 0,
                0, q * dt4 / 4, 0, q * dt3 / 2,
                q * dt3 / 2, 0, q * dt2, 0,
                0, q * dt3 / 2, 0, q * dt2
        );
        SimpleMatrix predictP = F.mult(P).mult(F.transpose()).plus(Q);
        SimpleMatrix H = new SimpleMatrix(2, 4, true,
                1, 0, 0, 0,
                0, 1, 0, 0
        );
        SimpleMatrix z = new SimpleMatrix(2, 1, true,
                curPos.x, curPos.y
        );
        double p2 = POSITION_NOISE * POSITION_NOISE;
        SimpleMatrix R = new SimpleMatrix(2, 2, true,
                p2, 0,
                0, p2
        );
        SimpleMatrix y = z.minus(H.mult(predictX));
        SimpleMatrix S = H.mult(predictP).mult(H.transpose()).plus(R);
        SimpleMatrix K = predictP.mult(H.transpose()).mult(S.invert());

        x = predictX.plus(K.mult(y));
        P = SimpleMatrix.identity(4).minus(K.mult(H)).mult(predictP);

        //---------------KF end------------------------
        lastMms = location.getTime() > 0 ? location.getTime() : System.currentTimeMillis();
        double res;
        double xPos = x.get(0, 0);
        double yPos = x.get(1, 0);
        if (lastPoint == null) {
            res = Math.sqrt(xPos * xPos + yPos * yPos);
            lastPoint = new Point();
        } else {
            double deltaX = x.get(0, 0) - lastPoint.x;
            double deltaY = x.get(1, 0) - lastPoint.y;
            res = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        }
        lastPoint.x = xPos;
        lastPoint.y = yPos;
        lastDistance += res;
        return lastDistance;
    }
}