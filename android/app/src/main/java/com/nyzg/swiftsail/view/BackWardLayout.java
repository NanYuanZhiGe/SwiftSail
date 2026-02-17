package com.nyzg.swiftsail.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.nyzg.swiftsail.R;

public class BackWardLayout extends ConstraintLayout {
    private final TextView mTitle;
    private @NonNull String mTitlePrefix;

    public BackWardLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.layout_backward, this, true);
        mTitle = findViewById(R.id.title);
        try (TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BackWardLayout)) {
            String temp = a.getString(R.styleable.BackWardLayout_bwTitle);
            mTitlePrefix = temp == null||temp.isBlank() ? "运动" : temp;
            mTitle.setText(mTitlePrefix);
        }
    }

    public void setTitlePrefix(@NonNull String mTitlePrefix) {
        this.mTitlePrefix = mTitlePrefix;
    }

    public void setTitleStatus(@NonNull String status) {
        SpannableString spannableString = new SpannableString(String.format("%s%s", mTitlePrefix, status));
        int len = mTitlePrefix.length();
        float smallSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, //sp
                getResources().getDisplayMetrics());
        spannableString.setSpan(new AbsoluteSizeSpan((int) smallSizePx), len, spannableString.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        mTitle.setText(spannableString);
    }
}
