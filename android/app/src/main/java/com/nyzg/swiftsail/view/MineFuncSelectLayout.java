package com.nyzg.swiftsail.view;

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
import androidx.core.content.res.ResourcesCompat;

import com.nyzg.swiftsail.R;

public class MineFuncSelectLayout extends ConstraintLayout {
    public MineFuncSelectLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.layout_mine_func_select, this, true);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MineFuncSelectLayout);
        ImageView funcIcon = findViewById(R.id.funcIcon);
        TextView funcName = findViewById(R.id.funcDesc);
        this.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.layout_mine_func_select, context.getTheme()));
        this.setPadding(0, 32, 0, 32);
        try {
            int resourceId = a.getResourceId(R.styleable.MineFuncSelectLayout_funcIcon, R.drawable.cancel);
            int tint = a.getResourceId(R.styleable.MineFuncSelectLayout_iconTint, R.color.black);
            funcIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), resourceId, context.getTheme()));
            funcIcon.setImageTintList(ContextCompat.getColorStateList(context, tint));

            String text = a.getString(R.styleable.MineFuncSelectLayout_funcName);
            funcName.setText(text);
            int textColor = a.getResourceId(R.styleable.MineFuncSelectLayout_textColor, R.color.black);
            if (textColor != R.color.black) {
                funcName.setTextColor(ResourcesCompat.getColor(getResources(), textColor, context.getTheme()));
            }
        } finally {
            a.recycle();
        }
    }
}
