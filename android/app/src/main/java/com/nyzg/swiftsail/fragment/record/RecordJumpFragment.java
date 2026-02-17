package com.nyzg.swiftsail.fragment.record;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.GlobalToast;
import com.nyzg.swiftsail.view.record.StageLayout;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecordJumpFragment extends RecordBaseFragment {
    public static Fragment getInstance() {
        return new RecordJumpFragment();
    }

    private class JumpListener implements SensorEventListener {
        final static private int FFT_SIZE = 64;
        double[] fftData = new double[FFT_SIZE];
        DoubleFFT_1D fft1D = new DoubleFFT_1D(FFT_SIZE);
        private int fftCount = 0;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
                return;
            }
            float az = event.values[2];
            if (az > 0) {
                az = Math.max(0, az - 9.8f);
            } else {
                az = az - 9.8f;
            }
            if (fftCount >= FFT_SIZE) {
                fft1D.realForward(fftData);
                //保留2-4hz的信息，然后进行反变换
                int fromIndex = 3;
                int toIndex = 5;

                // --- 1. 找最大幅度 ---
                int maxIndex = fromIndex;
                double maxMagnitude = 0;
                for (int i = fromIndex; i <= toIndex; i++) {

                    double real, imag;

                    // --- 2. 正确解析实部和虚部 (关键) ---
                    if (i == 0) {
                        // 直流分量，没有虚部
                        real = fftData[0];
                        imag = 0;
                    } else if (i == FFT_SIZE / 2) {
                        // 奈奎斯特频率，没有虚部
                        real = fftData[1];
                        imag = 0;
                    } else {
                        // 普通频率
                        // 打包规则：实部在 2*i, 虚部在 2*i+1
                        int realIndex = 2 * i;
                        int imagIndex = 2 * i + 1;

                        real = fftData[realIndex];
                        imag = fftData[imagIndex];
                    }

                    // 计算幅度
                    double magnitude = Math.sqrt(real * real + imag * imag);

                    if (magnitude > maxMagnitude) {
                        maxMagnitude = magnitude;
                        maxIndex = i; // 记录下标
                    }
                }
                double bestReal = 0, bestImag = 0;

                // --- 重新解析刚才找到的 maxIndex 的实部虚部，并保存 ---
                if (maxIndex == 0) {
                    bestReal = fftData[0];
                    bestImag = 0;
                } else if (maxIndex == FFT_SIZE / 2) {
                    bestReal = fftData[1];
                    bestImag = 0;
                } else {
                    bestReal = fftData[2 * maxIndex];
                    bestImag = fftData[2 * maxIndex + 1];
                }
                double energy = Math.sqrt(bestReal * bestReal + bestImag * bestImag);
                if (energy > 150) {
                    Arrays.fill(fftData, 0);
                    if (maxIndex == 0) {
                        fftData[0] = bestReal; // 直流分量
                    } else if (maxIndex == FFT_SIZE / 2) {
                        fftData[1] = bestReal; // 奈奎斯特
                    } else {
                        // 重点：这里必须按照 2*i, 2*i+1 的格式写回去
                        fftData[2 * maxIndex] = bestReal;
                        fftData[2 * maxIndex + 1] = bestImag;
                        // 注意：JTransforms 的 realInverse 会自动处理负频率的对称性
                    }
                    fft1D.realInverse(fftData, true);
                    for (int i = 1; i < FFT_SIZE; ++i) {
                        if ((fftData[i - 1] > 0 && fftData[i] < 0) || (fftData[i - 1] < 0 && fftData[i] > 0)) {
                            assert numberCount.getValue() != null;
                            numberCount.setValue(numberCount.getValue() + 1);
                        }
                    }
                    for (int i = 0; i < BAR_SIZE && i < FFT_SIZE; ++i) {
                        entries.get(i).setY((float) fftData[i]);
                    }
                } else {
                    for (int i = 0; i < BAR_SIZE; ++i) {
                        entries.get(i).setY(0f);
                    }
                }
                fftCount = 0;
            } else {
                fftData[fftCount] = az;
                ++fftCount;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    private MutableLiveData<Integer> numberCount = new MutableLiveData<>(0);
    private StageLayout stageLayout;
    private LineChart lineChart;

    private SensorManager sensorManager;
    private Sensor sensor;
    final private JumpListener jumpListener = new JumpListener();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean stopHandler = false;
    private final Runnable r = new Runnable() {
        @Override
        public void run() {
            if (stopHandler) {
                return;
            }
            lineChart.invalidate();
            handler.postDelayed(this, FLASH_INTERVAL);
        }
    };
    private final static int FLASH_INTERVAL = 1000;//ms
    private final static int BAR_SIZE = 32;
    private final List<Entry> entries;
    private final LineDataSet lineDataSet;

    public RecordJumpFragment() {
        entries = new ArrayList<>(BAR_SIZE);
        for (int i = 0; i < BAR_SIZE; ++i) {
            entries.add(new Entry(i, 0f));
        }
        lineDataSet = new LineDataSet(entries, "");
        lineDataSet.setCircleColors(ContextCompat.getColor(GlobalApplication.getAppContext(), R.color.heavyPink));
        lineDataSet.setColor(ContextCompat.getColor(GlobalApplication.getAppContext(), R.color.black));
        lineDataSet.setDrawFilled(true);
        lineDataSet.setFillDrawable(ContextCompat.getDrawable(GlobalApplication.getAppContext(), R.drawable.fragment_record_jump_chart_gradient));
        lineDataSet.setGradientColor(ContextCompat.getColor(GlobalApplication.getAppContext(), R.color.heavyGreen), ContextCompat.getColor(GlobalApplication.getAppContext(), R.color.white));
        lineDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private TextView jumpCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_record_jump, container, false);

        jumpCount = father.findViewById(R.id.t11);
        numberCount.observe(getViewLifecycleOwner(), i -> jumpCount.setText((i>>1) + ""));

        stageLayout = father.findViewById(R.id.stageLayout);
        stageLayout.setMCancel(this::myOnCancel);
        stageLayout.setMStart(this::myOnStart);
        stageLayout.setMStop(this::myOnStop);
        stageLayout.setMSubmit(this::myOnSubmit);

        lineChart = father.findViewById(R.id.lineChart);
        lineChart.setTouchEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setDescription(null);
        lineChart.getXAxis().setGranularity(1f);
        lineChart.getXAxis().setEnabled(true);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return "";
            }
        });
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getXAxis().setDrawAxisLine(false);
        lineChart.getAxisLeft().setEnabled(false);
        lineChart.getAxisRight().setDrawGridLines(false);
        lineChart.getAxisRight().setAxisMinimum(0f);
        lineChart.getAxisRight().setAxisMaximum(16f);

        LineData lineData = new LineData(lineDataSet);
        lineData.setDrawValues(false);
        lineChart.setData(lineData);
        lineChart.invalidate();
        handler.postDelayed(r, FLASH_INTERVAL);

        //获取加速度传感器
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (sensor != null) {
            sensorManager.registerListener(jumpListener, sensor, SensorManager.SENSOR_DELAY_GAME);
        } else {
            GlobalToast.COMMON_TOAST.accept("您的设备没有加速度传感器");
        }
        return father;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stopHandler = true;
        handler.removeCallbacks(r);
        if (sensor != null) {
            sensorManager.unregisterListener(jumpListener, sensor);
        }
    }

    private void myOnCancel() {
        numberCount.setValue(0);
    }

    private void myOnStart() {

    }

    private void myOnStop() {

    }

    private void myOnSubmit() {

    }


    @Override
    protected boolean needToAlert() {
        return true;
    }

    @Override
    protected boolean containsBackward() {
        return true;
    }

    @Override
    protected void onSaveQuit() {

    }
}