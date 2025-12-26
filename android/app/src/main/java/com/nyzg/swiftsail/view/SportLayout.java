package com.nyzg.swiftsail.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.listener.RecordSportSelectListener;
import com.nyzg.swiftsail.viewmodel.RecordSelectViewModel;

public class SportLayout extends ConstraintLayout {

    final private String type;
    @SuppressLint("ClickableViewAccessibility")
    public SportLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.sport_layout, this, true);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SportLayout);
        TextView textView = findViewById(R.id.sportName);
        ImageView imageView = findViewById(R.id.sportIcon);
        String type;
        try {
             type= a.getString(R.styleable.SportLayout_sportName);
            if (type == null || type.isEmpty()) {
                type = "未知项目";
            }
            textView.setText(type);
            int resource = a.getResourceId(R.styleable.SportLayout_sportIcon, R.drawable.add);
            imageView.setImageDrawable(ContextCompat.getDrawable(context, resource));
        } finally {
            a.recycle();
        }
        this.type=type;
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setRecordSelectViewModel(RecordSelectViewModel recordSelectViewModel) {
        this.setOnTouchListener(new RecordSportSelectListener(recordSelectViewModel,type));
    }
}
