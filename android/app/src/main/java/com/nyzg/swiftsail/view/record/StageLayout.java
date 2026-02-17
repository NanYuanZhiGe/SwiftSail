package com.nyzg.swiftsail.view.record;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;

public class StageLayout extends ConstraintLayout {
    private Runnable mCancel = () -> {
    };
    private Runnable mStart = () -> {
    };
    private Runnable mStop = () -> {
    };
    private Runnable mSubmit = () -> {
    };
    private boolean isStop = true;
    final private @NonNull ImageView mSubmitView;
    private final ImageView pauseOrResume;

    public StageLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.layout_stage, this, true);
        View cancel = findViewById(R.id.cancel);
        pauseOrResume = findViewById(R.id.pauseOrResume);
        mSubmitView = findViewById(R.id.submit);
        cancel.setOnClickListener(v -> {
            mStop.run();
            pauseOrResume.setImageDrawable(ContextCompat.getDrawable(GlobalApplication.getAppContext(), R.drawable.play));
            mCancel.run();
        });
        pauseOrResume.setOnClickListener(v -> {
            if (isStop) {
                isStop = false;
                pauseOrResume.setImageDrawable(ContextCompat.getDrawable(GlobalApplication.getAppContext(), R.drawable.pause));
                mStart.run();
            } else {
                isStop = true;
                pauseOrResume.setImageDrawable(ContextCompat.getDrawable(GlobalApplication.getAppContext(), R.drawable.play));
                mStop.run();
            }
        });
        //先暂定再提交
        mSubmitView.setOnClickListener(v -> {
            mStop.run();
            pauseOrResume.setImageDrawable(ContextCompat.getDrawable(GlobalApplication.getAppContext(), R.drawable.play));
            mSubmit.run();
        });
    }

    public void switchToPause(){
        isStop=true;
        mStop.run();
        pauseOrResume.setImageDrawable(ContextCompat.getDrawable(GlobalApplication.getAppContext(), R.drawable.play));
    }
    public void setSubmitDrawable(Drawable drawable) {
        mSubmitView.setImageDrawable(drawable);
    }

    public void setMCancel(@NonNull Runnable cancel) {
        mCancel = cancel;
    }

    public void setMStart(@NonNull Runnable start) {
        mStart = start;
    }

    public void setMStop(@NonNull Runnable stop) {
        mStop = stop;
    }

    public void setMSubmit(@NonNull Runnable submit) {
        mSubmit = submit;
    }
}
