package com.nyzg.swiftsail.fragment.record;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.view.SportLayout;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RecordSelectFragment extends Fragment {
    final public static Set<String> TYPE_SET = new HashSet<>();
    final public static String TYPE_WALK_OR_RUN = "走路/跑步";
    final public static String TYPE_BIKE = "骑行";
    final public static String TYPE_JUMP = "跳绳";
    final public static String TYPE_YOGA = "瑜伽";
    final public static String TYPE_BALL = "球类运动";

    static {
        TYPE_SET.add(TYPE_WALK_OR_RUN);
        TYPE_SET.add(TYPE_BIKE);
        TYPE_SET.add(TYPE_JUMP);
        TYPE_SET.add(TYPE_YOGA);
        TYPE_SET.add(TYPE_BALL);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_record_select, container, false);

        return father;
    }
}
