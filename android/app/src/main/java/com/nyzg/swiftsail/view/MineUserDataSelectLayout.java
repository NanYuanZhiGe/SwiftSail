package com.nyzg.swiftsail.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.nyzg.swiftsail.R;

public class MineUserDataSelectLayout extends ConstraintLayout {
    public MineUserDataSelectLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.layout_mine_user_data_select, this, true);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MineUserDataSelectLayout);
        TextView attribute = findViewById(R.id.attribute);
        try {
            attribute.setText(a.getString(R.styleable.MineUserDataSelectLayout_attributeName));
        } finally {
            a.recycle();
        }
        this.setClickable(true);
        this.setFocusable(true);
    }
}
