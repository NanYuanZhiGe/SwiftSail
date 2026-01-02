package com.nyzg.swiftsail.fragment.record;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.GlobalConf;
import com.nyzg.swiftsail.dbobj.Record;
import com.nyzg.swiftsail.repository.RecordRecordRepository;
import com.nyzg.swiftsail.repository.RecordRepository;
import com.nyzg.swiftsail.service.RecordRecordService;

import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RecordRecordFragment extends Fragment {
    private String type = "";
    final private static String TYPE_KEY = "type";
    private MapView mapView;
    private MapLibreMap mapLibreMap;
    private boolean premiseLocation = false;
    private boolean isPause = true;
    private ImageView pauseOrResumeButton;
    private ImageView cancelButton;
    private ImageView submitButton;
    private boolean isUnSave=false;
    private static volatile RecordRecordService recordService;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
    private static final ServiceConnection recordConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            RecordRecordService.MyBinder binder = (RecordRecordService.MyBinder) iBinder;
            recordService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            recordService = null;
        }
    };
    private static final String[] LOCATION_PERMISSIONS_28 = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.WAKE_LOCK
    };
    private static final String[] LOCATION_PERMISSIONS_29 = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.WAKE_LOCK
    };
    private static final String[] LOCATION_PERMISSIONS_34 = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_LOCATION,
            Manifest.permission.WAKE_LOCK
    };
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (checkPermission()) {
                    openLocation();
                } else {
                    Toast.makeText(GlobalApplication.getAppContext(), "使用运动记录需要您给App应用权限", Toast.LENGTH_LONG).show();
                }
            });

    public static Fragment getInstance(String type) {
        Fragment fragment = new RecordRecordFragment();
        if (type != null && RecordSelectFragment.TYPE_SET.contains(type)) {
            Bundle bundle = new Bundle();
            bundle.putString(TYPE_KEY, type);
            fragment.setArguments(bundle);
        }
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.type = getArguments() != null ? getArguments().getString(TYPE_KEY) : "";
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!isUnSave){
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    return;
                }
                new AlertDialog.Builder(requireContext())
                        .setTitle("")
                        .setMessage("您本次的运动记录未保存，需要保存它吗？")
                        .setPositiveButton("保存", (dialog, which) -> {
                            Record record = RecordRecordRepository.INSTANCE.getRecord();
                            saveSportData(record);
                            setEnabled(false);//禁用自己，避免无限递归
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        })
                        .setNegativeButton("不保存", (dialog, which) -> {
                            setEnabled(false);
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        }) // 取消则什么也不做
                        .show();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_record_record, container, false);
        TextView title = father.findViewById(R.id.title);
        //设置标题
        title.setText(this.type);
        //设置地图
        mapView = father.findViewById(R.id.mapView);
        mapView.getMapAsync(map -> {
            //保存引用
            mapLibreMap = map;
            // "https://demotiles.maplibre.org/style.json" 这个空白地图可以用于测试
            //10.84/23.1497/113.2996 zoom lat long
            //"http://10.252.115.37:5070/styles/basic-preview/style.json"
            //初始点位是广州的一个地方
            map.setStyle(GlobalConf.URL_TILE_SERVER);
            map.setCameraPosition(new CameraPosition.Builder()
                    .target(new LatLng(23.1479, 113.2996))
                    .zoom(16.72)
                    .build()
            );
        });
        //设置feet的三个按钮
        cancelButton = father.findViewById(R.id.cancel);
        cancelButton.setOnClickListener(this::onRecordCancel);
        this.pauseOrResumeButton = father.findViewById(R.id.pauseOrResume);
        pauseOrResumeButton.setOnClickListener(this::onRecordPauseAndResume);
        submitButton = father.findViewById(R.id.submit);
        this.submitButton.setOnClickListener(this::onRecordSubmit);
        if (checkPermission()) {
            openLocation();
        } else {
            acquirePermission();
        }
        //通过data layer来更新UI
        RecordRecordRepository repository = RecordRecordRepository.INSTANCE;
        repository.getMutableMinuteSecond().observe(getViewLifecycleOwner(), time -> ((TextView) father.findViewById(R.id.duration)).setText(time));
        repository.getMutableDistanceKilo().observe(getViewLifecycleOwner(), distance -> ((TextView) father.findViewById(R.id.distanceKilo)).setText(distance));
        repository.getMutableSpeedMeterSecond().observe(getViewLifecycleOwner(), speed -> ((TextView) father.findViewById(R.id.speed)).setText(speed));
        repository.getMutableLocation().observe(getViewLifecycleOwner(), location -> {
            if (mapLibreMap == null) {
                return;
            }
            mapLibreMap.easeCamera(
                    CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16.72),
                    1000 // 动画时长（毫秒）
            );
        });
        return father;
    }

    private boolean checkPermission() {
        if (this.getContext() == null) {
            return false;
        }
        int version = Build.VERSION.SDK_INT;
        if (version == Build.VERSION_CODES.P) {
            return checkPermissionList(RecordRecordFragment.LOCATION_PERMISSIONS_28);
        } else if (version < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return checkPermissionList(RecordRecordFragment.LOCATION_PERMISSIONS_29);
        } else {
            return checkPermissionList(RecordRecordFragment.LOCATION_PERMISSIONS_34);
        }
    }

    private boolean checkPermissionList(String[] list) {
        assert this.getContext() != null;
        for (String permission : list) {
            if (ContextCompat.checkSelfPermission(this.getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void acquirePermission() {
        int version = Build.VERSION.SDK_INT;
        if (version == Build.VERSION_CODES.P) {
            locationPermissionLauncher.launch(getUnGrantedPermission(RecordRecordFragment.LOCATION_PERMISSIONS_28));
        } else if (version < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            locationPermissionLauncher.launch(getUnGrantedPermission(RecordRecordFragment.LOCATION_PERMISSIONS_29));
        } else {
            locationPermissionLauncher.launch(getUnGrantedPermission(RecordRecordFragment.LOCATION_PERMISSIONS_34));
        }
    }

    private String[] getUnGrantedPermission(String[] list) {
        List<String> permissions = new ArrayList<>(list.length);
        assert this.getContext() != null;
        for (String permission : list) {
            if (ContextCompat.checkSelfPermission(this.getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(permission);
            }
        }
        return permissions.toArray(new String[0]);
    }

    private void openLocation() {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("定位服务未开启")
                    .setMessage("请开启定位服务以使用此功能")
                    .setPositiveButton("去设置", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            premiseLocation = true;
        }
    }


    private void onRecordCancel(View view) {
        isUnSave=false;
        isPause = true;
        this.pauseOrResumeButton.setImageDrawable(ContextCompat.getDrawable(GlobalApplication.getAppContext(), R.drawable.play));
        if (recordService != null) {
            recordService.pause();
            recordService.cancel();
        }
    }

    private void onRecordPauseAndResume(View view) {
        if (isPause) {
            isPause = false;
            this.pauseOrResumeButton.setImageDrawable(ContextCompat.getDrawable(GlobalApplication.getAppContext(), R.drawable.pause));
            onRecordResume();
        } else {
            isPause = true;
            this.pauseOrResumeButton.setImageDrawable(ContextCompat.getDrawable(GlobalApplication.getAppContext(), R.drawable.play));
            onRecordPause();
        }
    }

    private void onRecordResume() {
        isUnSave=true;
        if (!this.premiseLocation) {
            Toast.makeText(GlobalApplication.getAppContext(), "应用权限不足，无法进行记录", Toast.LENGTH_LONG).show();
            return;
        }
        //如果不是第一次启动，就不要新建一个类了，直接调用里面的暂停方法
        if (recordService != null) {
            recordService.resume();
            return;
        }
        //如果是第一次启动，那么就要先创建这个前台服务
        Intent intent = new Intent(requireContext(), RecordRecordService.class);
        String key = "maxAccelerate";
        if (this.type.equals(RecordSelectFragment.TYPE_WALK_OR_RUN)) {
            intent.putExtra(key, 10.5f);
        } else if (this.type.equals(RecordSelectFragment.TYPE_BIKE)) {
            intent.putExtra(key, 6.5f);
        } else {
            intent.putExtra(key, 10f);
        }
        intent.putExtra("recordType", type.equals(RecordSelectFragment.TYPE_BIKE) ? "useWheel" : "useFeet");
        //API>=28，直接启动前台服务就行了
        requireContext().startForegroundService(intent);
        requireContext().bindService(intent, recordConnection, Context.BIND_AUTO_CREATE);
    }

    private void onRecordPause() {
        if (recordService != null) {
            recordService.pause();
        }
    }

    private void onRecordSubmit(View view) {
        //首先是调整应用图标
        isPause = true;
        this.pauseOrResumeButton.setImageDrawable(ContextCompat.getDrawable(GlobalApplication.getAppContext(), R.drawable.play));
        //禁用所有的按钮
        cancelButton.setOnClickListener(v -> {
        });
        pauseOrResumeButton.setOnClickListener(v -> {
        });
        submitButton.setOnClickListener(v -> {
        });
        //暂停服务
        if (recordService != null) {
            recordService.pause();
            recordService.summary();
            Record record = RecordRecordRepository.INSTANCE.getRecord();
            //提示用户是否要保存，允许用户取消
            if (record != null) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("提交运动记录")
                        .setMessage(String.format("您要提交从 %s 到 %s 这段时间的运动记录吗?",
                                LocalDateTime.ofInstant(Instant.ofEpochMilli(record.startTime), ZoneId.systemDefault()).format(DATE_TIME_FORMATTER),
                                LocalDateTime.now().format(DATE_TIME_FORMATTER)))
                        .setPositiveButton("确定", (dialog, which) -> {
                            saveSportData(record);
                            recordService.cancel();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        }
        //恢复提交按钮的点击事件
        cancelButton.setOnClickListener(this::onRecordCancel);
        pauseOrResumeButton.setOnClickListener(this::onRecordPauseAndResume);
        submitButton.setOnClickListener(this::onRecordSubmit);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //销毁前台服务
        if (recordService != null) {
            requireContext().unbindService(recordConnection);
            recordService.destroy();
            recordService = null;
        }
        //销毁mapView
        if (mapView != null) {
            mapView.onDestroy();
        }
        //清空viewModel，这个是一个静态对象
        RecordRecordRepository.INSTANCE.initValue();
    }

    /**
     * 这个函数需要异步执行。首先是写本地数据库是可以直接完成的，
     * 容易出现异常的是网络请求，这部分如果失败了，后续就提醒一下用户错误信息
     * 下一次提交的时候，会自动尝试全部同步。
     */
    private void saveSportData(Record record) {
        isUnSave=false;
        if (record==null){
            return;
        }
        RecordRepository.INSTANCE.submitRepositoryRecord(record);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mapView != null) {
            mapView.onStop();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }
}
