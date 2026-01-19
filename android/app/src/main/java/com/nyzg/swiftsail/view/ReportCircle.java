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

import com.nyzg.swiftsail.R;
import com.seosh817.circularseekbar.CircularSeekBar;

public class ReportCircle extends ConstraintLayout {
    public ReportCircle(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.layout_report_circle, this, true);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ReportCircle);
        try {
            float circleSize = a.getDimension(R.styleable.ReportCircle_circleSize, 180f);
            float imageSize = a.getDimension(R.styleable.ReportCircle_imageSize, 60f);
            float textSize = a.getDimension(R.styleable.ReportCircle_textSize, 24f);
            int circleBk = a.getResourceId(R.styleable.ReportCircle_circleBk, R.color.rainPurple);
            int circleBk2 = a.getResourceId(R.styleable.ReportCircle_circleBk2, R.color.darkPurple);
            int imageBk = a.getResourceId(R.styleable.ReportCircle_imageBk, R.drawable.moon);
            CircularSeekBar circularSeekBar = findViewById(R.id.circular_seek_bar);
            ImageView imageView = findViewById(R.id.innerImage);
            TextView textView = findViewById(R.id.textDesc);
            imageView.setImageTintList(ContextCompat.getColorStateList(context, circleBk2));
            imageView.setImageDrawable(ContextCompat.getDrawable(context, imageBk));
            textView.setTextSize(textSize);
            circularSeekBar.setTrackColor(ContextCompat.getColor(context, circleBk));
            circularSeekBar.setProgressGradientColorsArray(
                    new int[]{ContextCompat.getColor(context, circleBk2), ContextCompat.getColor(context, circleBk2)}
            );

            LayoutParams seekBarParams = (LayoutParams) circularSeekBar.getLayoutParams();
            seekBarParams.width = (int) circleSize;
            seekBarParams.height = (int) circleSize;
            circularSeekBar.setLayoutParams(seekBarParams);

            LayoutParams imageParams = (LayoutParams) imageView.getLayoutParams();
            imageParams.width = (int) imageSize;
            imageParams.height = (int) imageSize;
            imageView.setLayoutParams(imageParams);
        } finally {
            a.recycle();
        }
    }
}