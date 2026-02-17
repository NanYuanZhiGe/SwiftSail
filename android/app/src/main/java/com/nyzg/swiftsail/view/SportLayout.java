package com.nyzg.swiftsail.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.nyzg.swiftsail.R;

public class SportLayout extends ConstraintLayout {

    final protected String type;
    final protected String name;

    @SuppressLint("ClickableViewAccessibility")
    public SportLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initLayout(context);
        TextView textView = findViewById(R.id.sportName);
        ImageView imageView = findViewById(R.id.sportIcon);
        String type;
        try (TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SportLayout)) {
            type = a.getString(R.styleable.SportLayout_sportType);
            if (type == null || type.isEmpty()) {
                type = "Unknow";
            }
            name=a.getString(R.styleable.SportLayout_sportName);
            textView.setText(a.getString(R.styleable.SportLayout_sportName));
            int resource = a.getResourceId(R.styleable.SportLayout_sportIcon, R.drawable.add);
            imageView.setImageDrawable(ContextCompat.getDrawable(context, resource));
        }
        this.type = type;
    }

    protected void initLayout(@NonNull Context context) {
        LayoutInflater.from(context).inflate(R.layout.sport_layout, this, true);
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}