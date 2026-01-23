package com.nyzg.swiftsail.fragment;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

abstract public class BackPressPopFragment extends Fragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                setEnabled(false);
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(requireActivity(), callback);
    }
}