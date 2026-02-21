package com.nyzg.swiftsail.fragment.record;

import android.annotation.SuppressLint;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.GlobalToast;
import com.nyzg.swiftsail.bean.MyJsonSerializer;
import com.nyzg.swiftsail.obj.record.MyManualJump;

import java.util.Arrays;

public class RecordJumpFragment extends RecordBaseFragment {
    public static Fragment getInstance() {
        return new RecordJumpFragment();
    }


    private float vx = 0;
    private float vy = 0;
    private float vz = 1;

    private class RotationListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != ROTATION_TYPE) {
                return;
            }
            final float x = event.values[0];
            final float y = event.values[1];
            final float z = event.values[2];
            final float w = event.values[3];
            float[] vec = new float[]{x, y, z, w};
            float[] matrix = new float[9];
            SensorManager.getRotationMatrixFromVector(matrix, vec);

            vx = matrix[6];
            vy = matrix[7];
            vz = matrix[8];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    private class JumpListener implements SensorEventListener {
        final static private int FFT_SIZE = 16;
        double[] fftData = new double[FFT_SIZE];
        private int fftCount = 0;
        boolean hasSeenHighPeak = false;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != ACCELERATION_TYPE) {
                return;
            }
            float ax = event.values[0];
            float ay = event.values[1];
            float az = event.values[2];
            az = ax * vx + ay * vy + az * vz;
            if (fftCount >= FFT_SIZE) {
                int lastT = -1;
                for (int i = 1; i < FFT_SIZE - 1; ++i) {
                    fftData[i] = (fftData[i - 1] + fftData[i] + fftData[i + 1]) / 3;
                }
                for (int i = 0; i < FFT_SIZE; ++i) {
                    float value = (float) fftData[i];
                    if (!hasSeenHighPeak && value > 15.0f) {
                        hasSeenHighPeak = true;
                    } else if (hasSeenHighPeak && value < 5.0f) {
                        if ((lastT < 0 || (i - lastT) > 9)) {
                            Integer val = myViewModel.numberCount.getValue();
                            assert val != null;
                            myViewModel.numberCount.setValue(val + 1);
                            hasSeenHighPeak = false; // 重置
                            lastT = i;
                        }
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

    public static class MyViewModel extends ViewModel {
        private final MutableLiveData<Integer> numberCount = new MutableLiveData<>(0);
        private final MutableLiveData<Integer> timeMinute = new MutableLiveData<>(0);
    }

    private MyViewModel myViewModel;

    @Override
    protected void myOnCreated(@Nullable Bundle bundle) {
        super.myOnCreated(bundle);
        myViewModel = new ViewModelProvider(this).get(MyViewModel.class);
    }

    private TextView jumpCount;
    private SensorManager sensorManager;
    private Sensor sensor;
    private Sensor sensorRotate;
    final private JumpListener jumpListener = new JumpListener();
    final private RotationListener rotationListener = new RotationListener();
    private final static int SENSOR_RATE = 20000;//50Hz
    private final static int ACCELERATION_TYPE = Sensor.TYPE_ACCELEROMETER;
    private final static int ROTATION_TYPE = Sensor.TYPE_GAME_ROTATION_VECTOR;
    private boolean stopHandler = false;
    final private Handler handler = new Handler(Looper.getMainLooper());
    final private Runnable r = new Runnable() {
        @Override
        public void run() {
            if (stopHandler) {
                return;
            }
            assert myViewModel.timeMinute.getValue() != null;
            myViewModel.timeMinute.setValue(myViewModel.timeMinute.getValue() + 1);
            handler.postDelayed(this, 1000);
        }
    };

    @SuppressLint("DefaultLocale")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_record_jump, container, false);
        TextView bigCount = father.findViewById(R.id.bigCount);
        TextView seconds = father.findViewById(R.id.t21);
        TextView numberPerSec = father.findViewById(R.id.t31);

        jumpCount = father.findViewById(R.id.t11);
        myViewModel.numberCount.observe(getViewLifecycleOwner(), i -> {
            jumpCount.setText(String.format("%d", i));
            bigCount.setText(String.format("%d", i));
        });
        myViewModel.timeMinute.observe(getViewLifecycleOwner(), i -> {
            seconds.setText(String.format("%d", i));
            if (i == 0) {
                numberPerSec.setText("0");
            } else {
                assert myViewModel.numberCount.getValue() != null;
                numberPerSec.setText(String.format("%.0f", myViewModel.numberCount.getValue() / 60.0 / i));
            }
        });


        //获取加速度传感器
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(ACCELERATION_TYPE);
        sensorRotate = sensorManager.getDefaultSensor(ROTATION_TYPE);
        return father;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensor != null) {
            sensorManager.unregisterListener(jumpListener, sensor);
        }
        if (sensorRotate != null) {
            sensorManager.unregisterListener(rotationListener, sensorRotate);
        }
    }

    @Override
    protected void doStop() {
        if (sensor != null) {
            sensorManager.unregisterListener(jumpListener, sensor);
        }
        if (sensorRotate != null) {
            sensorManager.unregisterListener(rotationListener, sensorRotate);
        }
        Arrays.fill(jumpListener.fftData, 0);
        stopHandler = true;
        handler.removeCallbacks(r);
    }

    @Override
    protected boolean doStart() {
        if (sensor != null) {
            sensorManager.registerListener(jumpListener, sensor, SENSOR_RATE);
        } else {
            GlobalToast.COMMON_TOAST.accept("您的设备没有加速度传感器");
        }
        if (sensorRotate != null) {
            sensorManager.registerListener(rotationListener, sensorRotate, SENSOR_RATE);
        } else {
            GlobalToast.COMMON_TOAST.accept("您的设备缺少角度测量器，计算结果可能不够精准");
        }
        stopHandler = false;
        handler.post(r);
        return true;
    }

    @Override
    protected void reset() {
        Arrays.fill(jumpListener.fftData, 0);
        myViewModel.numberCount.setValue(0);
        myViewModel.timeMinute.setValue(0);
    }

    @Override
    protected void summary() {
        assert myViewModel.numberCount.getValue() != null;
        final int val = myViewModel.numberCount.getValue();
        this.mViewModel.mExposeValue = val;
        this.mViewModel.mDetailValue = MyJsonSerializer.serialize(new MyManualJump(val));
    }

    @Override
    protected boolean alertOnCancel() {
        return false;
    }
}