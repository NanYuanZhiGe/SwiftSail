package com.nyzg.swiftsail.obj.record;

public class DistanceSimple extends DistanceAlgorithm {
    private double accumulateDistance = 0;//单位米
    private boolean noLastG = true;
    private double lastGx = 0;
    private double lastGy = 0;
    private long lastTime;
    private double lastSpeed;
    private final double MAX_ACCELERATE;

    public DistanceSimple(double MAX_ACCELERATE) {
        super();
        this.MAX_ACCELERATE = MAX_ACCELERATE;
        init();
    }

    private void init() {
        accumulateDistance = 0;
        noLastG = true;
        lastGx = 0;
        lastGy = 0;
        lastTime = 0L;
        lastSpeed = 0;
    }

    @Override
    public double update(double gx, double gy) {
        double res = 0;
        if (noLastG) {
            lastGx = gx;
            lastGy = gy;
            lastTime = System.currentTimeMillis();
            lastSpeed = 0;
            noLastG = false;
        } else {
            double deltaX = gx - lastGx;
            double deltaY = lastGy;
            double deltaDistance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);//米，这个不一定准确
            long currentTime = System.currentTimeMillis();
            double deltaTime = (currentTime - lastTime) / 1000.0;//秒，这个一定准确
            //检查位移是否合法
            //deltaDistance<=(lastSpeed+maxAccelerate*deltaTime)*deltaTime*0.5
            double legalDistance = (lastSpeed + MAX_ACCELERATE * deltaTime) * deltaTime * 0.5;
            if (deltaDistance > legalDistance) {//不合法，使用上一次的速度*时间记录位移
                deltaDistance = lastSpeed * deltaTime;
            }
            //如果用户的速度很小，比如0.2m/s，这个时候就认为是小波动，不会累计数据
            //此时可能是用户在某一个地方停留了很久
            //防止deltaTime过小，如果时间过小，认为速度为0
            double currentSpeed = deltaTime > 1e-3 ? deltaDistance / deltaTime : .0;
            if (currentSpeed >= 0.2) {
                accumulateDistance += deltaDistance;
                res = deltaDistance;
            }
            //更新上一次的数据
            lastGx = gx;
            lastGy = gy;
            lastTime = currentTime;
            lastSpeed = currentSpeed;
        }
        return res;
    }

    @Override
    public double summary() {
        return accumulateDistance;
    }

    @Override
    protected void reset0() {
        init();
    }
}
