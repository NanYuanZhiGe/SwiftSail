package com.nyzg.swiftsail.fragment.report;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.nyzg.swiftsail.fragment.BackPressQuitFragment;

abstract public class ReportDetailBase extends BackPressQuitFragment {
    final protected static String LAYOUT_KEY = "layout_key";
    private int layoutResource;

    @Override
    final public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = this.getArguments();
        assert bundle != null;
        this.layoutResource = bundle.getInt(LAYOUT_KEY);
    }

    @Nullable
    @Override
    final public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(layoutResource, container, false);
        init(father);
        return father;
    }

    protected abstract void init(View father);
}
