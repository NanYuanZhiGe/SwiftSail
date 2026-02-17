package com.nyzg.swiftsail.obj.record;

import androidx.annotation.Keep;

import org.ejml.simple.SimpleMatrix;

@Keep
public class DistanceKF extends DistanceAlgorithm {
    private SimpleMatrix xk = new SimpleMatrix(4, 1);
    final private SimpleMatrix H = new SimpleMatrix(2, 4);
    final private SimpleMatrix HT;
    final private SimpleMatrix Q = new SimpleMatrix(4, 4);
    final private SimpleMatrix R = new SimpleMatrix(2, 2);
    private SimpleMatrix Pk = new SimpleMatrix(4, 4);
    final private SimpleMatrix I = SimpleMatrix.diag(1, 1, 1, 1);
    private long lastUpdateTime = -1L;
    private double mAccumulateDistance;

    public DistanceKF(double gx, double gy) {
        //初始状态向量
        xk.setRow(0, 0, gx);
        xk.setRow(1, 0, gy);
        xk.setRow(2, 0, 0);
        xk.setRow(3, 0, 0);
        //位置误差10m，速度误差10m
        Pk.setRow(0, 0, 100, 0, 0, 0);
        Pk.setRow(1, 0, 0, 100, 0, 0);
        Pk.setRow(2, 0, 0, 0, 4, 0);
        Pk.setRow(3, 0, 0, 0, 0, 4);

        //噪声
        double q = 1.0;
        Q.set(0, 0, 0.25 * Math.pow(0.1, 4) * q);
        Q.set(1, 1, 0.25 * Math.pow(0.1, 4) * q);
        Q.set(2, 2, Math.pow(0.1, 2) * q);
        Q.set(3, 3, Math.pow(0.1, 2) * q);

        //观测矩阵
        H.setRow(0, 0, 1, 0, 0, 0);
        H.setRow(1, 0, 0, 1, 0, 0);
        HT = H.transpose();

        //GPS 的不确定性
        R.setRow(0, 0, 25, 0);
        R.setRow(1, 0, 0, 25);
    }

    private void init() {

    }

    public void predict(double ax, double ay) {
        if (lastUpdateTime == -1L) {//此时没有时间意义，不需要理
            lastUpdateTime = System.currentTimeMillis();
            return;
        }
        long currentTime = System.currentTimeMillis();
        double deltaT = (currentTime - lastUpdateTime) / 1000.0;//单位是秒
        lastUpdateTime = currentTime;
        //状态转移矩阵
        SimpleMatrix F = new SimpleMatrix(4, 4);
        F.setRow(0, 0, 1, 0, deltaT, 0);
        F.setRow(1, 0, 0, 1, 0, deltaT);
        F.setRow(2, 0, 0, 0, 1, 0);
        F.setRow(3, 0, 0, 0, 0, 1);
        SimpleMatrix B = new SimpleMatrix(4, 2);
        //控制矩阵
        double halfDeltaTSquare = deltaT * deltaT * 0.5;
        B.setRow(0, 0, halfDeltaTSquare, 0);
        B.setRow(1, 0, 0, halfDeltaTSquare);
        B.setRow(2, 0, deltaT, 0);
        B.setRow(3, 0, 0, deltaT);
        SimpleMatrix uk = new SimpleMatrix(2, 1);
        uk.setRow(0, 0, ax);
        uk.setRow(1, 0, ay);
        xk = F.mult(xk).plus(B.mult(uk));
        SimpleMatrix FT = F.transpose();
        Pk = F.mult(Pk).mult(FT).plus(Q);
    }

    /**
     *
     * @return 返回-1表示坐标不对
     */
    public double update(double gx, double gy) {
        SimpleMatrix z = new SimpleMatrix(2, 1);
        z.set(0, 0, gx);
        z.set(1, 0, gy);
        SimpleMatrix y = z.minus(H.mult(xk));

        SimpleMatrix S = H.mult(Pk).mult(HT).plus(R);
        SimpleMatrix K = Pk.mult(HT).mult(S.invert());

        double oldGx = xk.get(0, 0);
        double oldGy = xk.get(1, 0);
        xk = xk.plus(K.mult(y));
        double newGx = xk.get(0, 0);
        double newGy = xk.get(1, 0);
        SimpleMatrix I_KH = I.minus(K.mult(H));
        Pk = I_KH.mult(Pk).mult(I_KH.transpose()).plus(K.mult(R).mult(K.transpose()));

        double deltaGx = newGx - oldGx;
        double deltaGy = newGy - oldGy;
        double deltaDistance = Math.sqrt(deltaGx * deltaGx + deltaGy * deltaGy);
        mAccumulateDistance += deltaDistance;
        return deltaDistance;
    }

    @Override
    public double summary() {
        return mAccumulateDistance;
    }

    @Override
    protected void reset0() {
        init();
    }
}