package com.nyzg.swiftsail.fragment.geo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nyzg.swiftsail.R;

public class GeoNotificationFragment extends Fragment {
    public static Fragment getInstance(){
        return new GeoNotificationFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father=inflater.inflate(R.layout.fragment_geo_notification,container,false);
        return father;
    }
}