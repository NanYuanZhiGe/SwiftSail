package com.nyzg.swiftsail.fragment.record;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.ServerURL;
import com.nyzg.swiftsail.repository.RecordRecordRepository;
import com.nyzg.swiftsail.service.RecordRecordService;

import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;

import java.util.ArrayList;
import java.util.List;


public class RecordRecordFragment extends RecordBaseFragment {
    private MapView mapView;
    private MapLibreMap mapLibreMap;
    private boolean premiseLocation = false;
    private static volatile RecordRecordService recordService;
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

    @SuppressLint("InlinedApi")//api的问题之后会处理
    private static final String[] LOCATION_PERMISSIONS_29 = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.WAKE_LOCK
    };
    @SuppressLint("InlinedApi")//api的问题后面会if区分
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
                    Toast.makeText(GlobalApplication.getAppContext(), "使用运动记录需要您给App应用权限", Toast.LENGTH_SHORT).show();
                }
            });

    public static Fragment getInstance() {
        return new RecordRecordFragment();
    }


    final private MutableLiveData<String> minuteSecond = new MutableLiveData<>();
    final private MutableLiveData<String> distanceKilo = new MutableLiveData<>();
    final private MutableLiveData<String> speedMeterSecond = new MutableLiveData<>();

    @Override
    protected void myOnCreated(@Nullable Bundle bundle) {
        super.myOnCreated(bundle);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_record_record, container, false);
        //设置地图
        mapView = father.findViewById(R.id.mapView);
        mapView.getMapAsync(map -> {
            //保存引用
            mapLibreMap = map;
            // "https://demotiles.maplibre.org/style.json" 这个空白地图可以用于测试
            //10.84/23.1497/113.2996 zoom lat long
            //"http://10.252.115.37:5070/styles/basic-preview/style.json"
            //初始点位是广州的一个地方
            map.setStyle(ServerURL.URL_TILE_SERVER);
            map.setCameraPosition(new CameraPosition.Builder()
                    .target(new LatLng(23.1479, 113.2996))
                    .zoom(16.72)
                    .build()
            );
        });
        //检查权限
        if (checkPermission()) {
            openLocation();
        } else {
            acquirePermission();
        }
        //通过data layer来更新UI
        minuteSecond.observe(getViewLifecycleOwner(), time -> ((TextView) father.findViewById(R.id.duration)).setText(time));
        distanceKilo.observe(getViewLifecycleOwner(), distance -> ((TextView) father.findViewById(R.id.distanceKilo)).setText(distance));
        speedMeterSecond.observe(getViewLifecycleOwner(), speed -> ((TextView) father.findViewById(R.id.speed)).setText(speed));
        RecordRecordRepository.getInstance().locationGlobal.observe(getViewLifecycleOwner(), location -> {
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

    @Override
    protected void doStop() {
        onRecordPause();
    }

    @Override
    protected boolean doStart() {
        return onRecordResume();
    }

    @Override
    protected void reset() {
        minuteSecond.setValue("00:00");
        distanceKilo.setValue("0");
        speedMeterSecond.setValue("0");
        RecordRecordRepository.getInstance().exposeRecord0.setValue(0L);
        RecordRecordRepository.getInstance().detailRecord0.setValue(null);
        if (recordService != null) {
            recordService.reset();
        }
    }

    @Override
    protected void summary() {
        //暂停服务
        if (recordService != null) {
            recordService.summary();
        }
        //理论上不可能为空的
        if (RecordRecordRepository.getInstance().exposeRecord0.getValue() == null) {
            return;
        }
        mViewModel.mExposeValue = RecordRecordRepository.getInstance().exposeRecord0.getValue();
        mViewModel.mDetailValue = RecordRecordRepository.getInstance().detailRecord0.getValue();
    }


    private boolean onRecordResume() {
        if (!this.premiseLocation) {
            //再次检查，因为用户可能切出去打开
            LocationManager locationManager = (LocationManager) requireContext()
                    .getSystemService(Context.LOCATION_SERVICE);
            this.premiseLocation = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (!premiseLocation) {
                Toast.makeText(GlobalApplication.getAppContext(), "应用权限不足，无法进行记录", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        //如果不是第一次启动，就不要新建一个类了，直接调用里面的暂停方法
        if (recordService != null) {
            recordService.resume();
            return true;
        }
        //如果是第一次启动，那么就要先创建这个前台服务
        Intent intent = new Intent(requireContext(), RecordRecordService.class);
        //API>=28，直接启动前台服务就行了
        requireContext().startForegroundService(intent);
        requireContext().bindService(intent, recordConnection, Context.BIND_AUTO_CREATE);
        return true;
    }

    private void onRecordPause() {
        if (recordService != null) {
            recordService.pause();
        }
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
