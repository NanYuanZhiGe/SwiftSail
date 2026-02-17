package com.nyzg.swiftsail.fragment.record;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nyzg.swiftsail.R;

public class RecordSelectFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_record_select, container, false);
        father.findViewById(R.id.backward).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        return father;
    }
}
