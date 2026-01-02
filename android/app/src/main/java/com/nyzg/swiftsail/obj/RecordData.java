package com.nyzg.swiftsail.obj;

public class RecordData {
    private int[] useFeetHour;
    private int[] useWheelHour;
    private int steps = 0;
    private float useFeetMeters = 0f;
    private float useWheelMeters = 0f;

    private float consumption = 0f;

    public RecordData() {
        useFeetHour = new int[6];
        useWheelHour = new int[6];
    }

    public RecordData(RecordData data) {
        this.useFeetHour = data.useFeetHour;
        this.useWheelHour = data.useWheelHour;
        this.steps = data.steps;
        this.useFeetMeters = data.useFeetMeters;
        this.useWheelMeters = data.useWheelMeters;
        this.consumption = data.consumption;
    }

    public void setFeetData(int startHour, int endHour, float meters, float durationSecond) {
        startHour /= 4;
        endHour /= 4;
        for (int i = startHour; i <= endHour; ++i) {
            useFeetHour[i] += 1;
        }
        useFeetMeters = meters;
        consumption = (float) (600 * durationSecond / 3600.0);
        float AVERAGE_STEP_LEN = 0.76f;
        steps = (int) (meters / AVERAGE_STEP_LEN);
    }

    public void setWheelData(int startHour, int endHour, float meters, float durationSecond) {
        startHour /= 4;
        endHour /= 4;
        for (int i = startHour; i <= endHour; ++i) {
            useWheelHour[i] += 1;
        }
        useWheelMeters = meters;
        consumption = (float) (400 * durationSecond / 3600.0);
    }

    public void updateFeetData(int startHour, int endHour, float meters, float durationSecond) {
        /*
        假设有一个人从8:34运动到13:02，那么startHour就是8，endHour就是13
        （8/4）=2，是第三个区间
        （13/4）=3，是第四个区间
         */
        startHour /= 4;
        endHour /= 4;
        for (int i = startHour; i <= endHour; ++i) {
            useFeetHour[i] += 1;
        }
        useFeetMeters += meters;
        consumption += 600 * durationSecond / 3600.0;
        float AVERAGE_STEP_LEN = 0.76f;
        steps += meters / AVERAGE_STEP_LEN;
    }

    public void updateWheelData(int startHour, int endHour, float meters, float durationSecond) {
        /*
        假设有一个人从8:34运动到13:02，那么startHour就是8，endHour就是13
        （8/4）=2，是第三个区间
        （13/4）=3，是第四个区间
         */
        startHour /= 4;
        endHour /= 4;
        for (int i = startHour; i <= endHour; ++i) {
            useWheelHour[i] += 1;
        }
        useWheelMeters += meters;
        consumption += 400 * durationSecond / 3600.0;
    }

    //第一个是走路/跑步，第二个是骑行
    public Pair<float[], float[]> getPercentage() {
        float total = 0f;
        for (int i = 0; i < 6; ++i) {
            total += useFeetHour[i];
            total += useWheelHour[i];
        }
        float[] a = new float[6];
        float[] b = new float[6];
        if (Math.abs(total) < 1e-3) {
            return new Pair<>(a, b);
        }
        for (int i = 0; i < 6; ++i) {
            a[i] = useFeetHour[i] / total;
            b[i] = useWheelHour[i] / total;
        }
        return new Pair<>(a, b);
    }

    public int getSteps() {
        return steps;
    }

    public float getConsumption() {
        return consumption;
    }

    public float getKiloMeters() {
        return (useFeetMeters + useWheelMeters) / 1000;
    }
}
