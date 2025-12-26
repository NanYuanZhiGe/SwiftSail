package com.nyzg.swiftsail.fragment.record;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nyzg.swiftsail.R;

import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapView;

public class RecordRecordFragment extends Fragment {
    private String type = "";
    final private static String TYPE_KEY = "type";
    private MapView mapView;

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
        title.setText(this.type);
        mapView = father.findViewById(R.id.mapView);
        mapView.getMapAsync(map -> {
            //"https://demotiles.maplibre.org/style.json" 这个空白地图可以用于测试
            //10.84/23.1497/113.2996 zoom lat long
            map.setStyle("http://10.252.115.37:5070/styles/basic-preview/style.json");
            map.setCameraPosition(new CameraPosition.Builder()
                    .target(new LatLng(23.1479, 113.2996))
                    .zoom(10.84)
                    .build()
            );
        });
        return father;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }
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
