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

public class ReportMain0Rect extends ConstraintLayout {
    private final TextView content;
    private final TextView content1;
    private final ReportCircle circle;

    public ReportMain0Rect(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.layout_report_main0_rect, this, true);
        try (TypedArray a = getResources().obtainAttributes(attrs, R.styleable.ReportMain0Rect)) {
            TextView title1 = findViewById(R.id.title1);
            content = findViewById(R.id.content);
            content1 = findViewById(R.id.content1);
            circle = findViewById(R.id.circle);
            String title1Text = a.getString(R.styleable.ReportMain0Rect_title1Text);
            String contentText = a.getString(R.styleable.ReportMain0Rect_contentText);
            String content1Text = a.getString(R.styleable.ReportMain0Rect_content1Text);
            int circleBk = a.getResourceId(R.styleable.ReportMain0Rect_rCircleBk, R.color.rainPurple);
            int circleBk2 = a.getResourceId(R.styleable.ReportMain0Rect_rCircleBk2, R.color.heavyPurple);
            int imageBk = a.getResourceId(R.styleable.ReportMain0Rect_rImageBk, R.drawable.moon);
            title1.setText(title1Text);
            content.setText(contentText);
            content1.setText(content1Text);
            circle.setCircleBk(circleBk);
            circle.setCircleBk2(circleBk2);
            circle.setCircleImageBk(imageBk, circleBk2);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateUi(String content, String content1, float progress) {
        this.content.setText(content);
        this.content1.setText(content1);
        this.circle.setProgress(progress);
    }
}
