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
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        circularSeekBar.setTrackColor(trackColor);
        circularSeekBar.setBarWidth(barWidth);
        circularSeekBar.setProgressGradientColorsArray(progressColorArray);
        circularSeekBar.setLayoutParams(seekLayoutParam);
    }

    final private CircularSeekBar circularSeekBar;
    final private float barWidth;
    private int trackColor;
    private int[] progressColorArray;
    private final LayoutParams seekLayoutParam;
    private final ImageView innerImageView;
    private final TextView textDesc;

    public ReportCircle(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        LayoutInflater.from(context).inflate(R.layout.layout_report_circle, this, true);
        try (TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ReportCircle)) {
            float circleSize = a.getDimension(R.styleable.ReportCircle_circleSize, 180f);
            float imageSize = a.getDimension(R.styleable.ReportCircle_imageSize, 60f);
            float textSize = a.getDimension(R.styleable.ReportCircle_textSize, 24f);
            int circleBk = a.getResourceId(R.styleable.ReportCircle_circleBk, R.color.rainPurple);
            int circleBk2 = a.getResourceId(R.styleable.ReportCircle_circleBk2, R.color.darkPurple);
            int imageBk = a.getResourceId(R.styleable.ReportCircle_imageBk, R.drawable.moon);
            String detailName = a.getString(R.styleable.ReportCircle_detailName);
            textDesc = findViewById(R.id.textDesc);
            ((TextView) findViewById(R.id.name)).setText(detailName);
            barWidth = a.getFloat(R.styleable.ReportCircle_circleBarWidth, 14f);
            circularSeekBar = findViewById(R.id.circular_seek_bar);
            innerImageView = findViewById(R.id.innerImage);
            TextView textView = findViewById(R.id.textDesc);
            innerImageView.setImageTintList(ContextCompat.getColorStateList(context, circleBk2));
            innerImageView.setImageDrawable(ContextCompat.getDrawable(context, imageBk));
            textView.setTextSize(textSize);
            trackColor = ContextCompat.getColor(context, circleBk);
            progressColorArray = new int[]{ContextCompat.getColor(context, circleBk2), ContextCompat.getColor(context, circleBk2)};

            seekLayoutParam = (LayoutParams) circularSeekBar.getLayoutParams();
            seekLayoutParam.width = (int) circleSize;
            seekLayoutParam.height = (int) circleSize;

            LayoutParams imageParams = (LayoutParams) innerImageView.getLayoutParams();
            imageParams.width = (int) imageSize;
            imageParams.height = (int) imageSize;
            innerImageView.setLayoutParams(imageParams);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setCircleBk(int resourceId) {
        trackColor = ContextCompat.getColor(getContext(), resourceId);
        circularSeekBar.setTrackColor(trackColor);
    }

    public void setCircleBk2(int resourceId) {
        progressColorArray = new int[]{ContextCompat.getColor(getContext(), resourceId), ContextCompat.getColor(getContext(), resourceId)};
        circularSeekBar.setProgressGradientColorsArray(progressColorArray);
    }

    public void setCircleImageBk(int resourceId, int colorId) {
        innerImageView.setImageTintList(ContextCompat.getColorStateList(getContext(), colorId));
        innerImageView.setImageDrawable(ContextCompat.getDrawable(getContext(), resourceId));
    }

    public void updateUi(String text, float progress) {
        textDesc.setText(text);
        circularSeekBar.setProgress(progress);
    }

    public void setProgress(float progress) {
        circularSeekBar.setProgress(progress);
    }
}