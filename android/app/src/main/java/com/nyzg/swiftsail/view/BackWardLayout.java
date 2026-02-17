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

public class BackWardLayout extends ConstraintLayout {
    public BackWardLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.layout_backward, this, true);
        TextView textView = findViewById(R.id.title);
        try (TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BackWardLayout)) {
            textView.setText(a.getString(R.styleable.BackWardLayout_bwTitle));
        }
    }
}
