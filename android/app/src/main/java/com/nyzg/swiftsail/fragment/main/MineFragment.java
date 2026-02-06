package com.nyzg.swiftsail.fragment.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.fragment.mine.MineMainFragment;

public class MineFragment extends MainBase {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.layout_mine, container, false);
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.mineFragment, MineMainFragment.getInstance())
                .commit();
        return father;
    }
}
