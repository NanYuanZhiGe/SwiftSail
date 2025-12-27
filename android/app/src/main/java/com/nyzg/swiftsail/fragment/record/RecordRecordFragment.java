package com.nyzg.swiftsail.fragment.record;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.service.RecordRecordService;
import com.nyzg.swiftsail.viewmodel.RecordRecordViewModel;

import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.Image;
import org.maplibre.android.maps.MapView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RecordRecordFragment extends Fragment {
    private String type = "";
    final private static String TYPE_KEY = "type";
    private MapView mapView;
    private boolean premiseLocation = false;
    private boolean isPause = true;
    private ImageView imageView;
    private static RecordRecordService recordService;
    public static RecordRecordViewModel recordViewModel;
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
                new AlertDialog.Builder(requireContext())
                        .setTitle("")
                        .setMessage("您本次的运动记录未保存，需要保存它吗？")
                        .setPositiveButton("保存", (dialog, which) -> {
                            saveSportData();
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
            // "https://demotiles.maplibre.org/style.json" 这个空白地图可以用于测试
            //10.84/23.1497/113.2996 zoom lat long
            //"http://10.252.115.37:5070/styles/basic-preview/style.json"
            //初始点位是广州的一个地方
            map.setStyle("https://demotiles.maplibre.org/style.json");
            map.setCameraPosition(new CameraPosition.Builder()
                    .target(new LatLng(23.1479, 113.2996))
                    .zoom(10.84)
                    .build()
            );
        });
        //设置feet的三个按钮
        father.findViewById(R.id.cancel).setOnClickListener(this::onRecordCancel);
        this.imageView = father.findViewById(R.id.pauseOrResume);
        imageView.setOnClickListener(this::onRecordPauseAndResume);
        father.findViewById(R.id.submit).setOnClickListener(this::onRecordSubmit);
        if (checkPermission()) {
            openLocation();
        } else {
            acquirePermission();
        }
        recordViewModel = new ViewModelProvider(this).get(RecordRecordViewModel.class);
        recordViewModel.getTimeMinuteSecond().observe(getViewLifecycleOwner(), time -> ((TextView) father.findViewById(R.id.duration)).setText(time));
        recordViewModel.getDistanceKiloMeter().observe(getViewLifecycleOwner(), distance -> ((TextView) father.findViewById(R.id.distanceKilo)).setText(distance));
        recordViewModel.getSpeedMeterSecond().observe(getViewLifecycleOwner(), speed -> ((TextView) father.findViewById(R.id.speed)).setText(speed));
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
        if (recordViewModel != null) {
            recordViewModel.initDefaultValue();
        }
    }

    private void onRecordPauseAndResume(View view) {
        if (isPause) {
            isPause = false;
            this.imageView.setImageDrawable(ContextCompat.getDrawable(GlobalApplication.getAppContext(), R.drawable.pause));
            onRecordResume();
        } else {
            isPause = true;
            this.imageView.setImageDrawable(ContextCompat.getDrawable(GlobalApplication.getAppContext(), R.drawable.play));
            onRecordPause();
        }
    }

    private void onRecordResume() {
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
        //API>=28，直接启动前台服务就行了
        requireContext().startForegroundService(intent);
    }

    private void onRecordPause() {
        if (recordService != null) {
            recordService.pause();
        }
    }

    private void onRecordSubmit(View view) {
        isPause = true;
        this.imageView.setImageDrawable(ContextCompat.getDrawable(GlobalApplication.getAppContext(), R.drawable.play));
        if (recordViewModel != null) {
            recordViewModel.initDefaultValue();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //销毁mapView
        if (mapView != null) {
            mapView.onDestroy();
        }
        //销毁前台服务
        if (recordService != null) {
            recordService.destroy();
        }
        recordService = null;
        //清空viewModel，这个是一个静态对象
        recordViewModel = null;
    }

    private void saveSportData() {

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
