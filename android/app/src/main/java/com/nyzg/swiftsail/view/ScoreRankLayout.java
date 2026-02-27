package com.nyzg.swiftsail.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.iarcuschin.simpleratingbar.SimpleRatingBar;
import com.nyzg.swiftsail.R;

public class ScoreRankLayout extends ConstraintLayout {

    private TextView scoreName;
    private SimpleRatingBar score;

    @SuppressLint("ClickableViewAccessibility")
    public ScoreRankLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.layout_score_rank, this, true);
        scoreName = findViewById(R.id.scoreName);
        score = findViewById(R.id.score);
        try (TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ScoreRankLayout)) {
            scoreName.setText(a.getString(R.styleable.ScoreRankLayout_scoreNameText));
            scoreName.setTextSize(a.getDimensionPixelSize(R.styleable.ScoreRankLayout_scoreNameTextSize, 16));
            score.setStarSize(a.getDimensionPixelSize(R.styleable.ScoreRankLayout_scoreViewLength, 24));
            boolean scoreStatic = a.getBoolean(R.styleable.ScoreRankLayout_scoreStatic, true);
            if (scoreStatic) {
                score.setOnTouchListener((v, event) -> true);
            }
            setScore(0f);
        }
    }

    public void setScore(float f) {
        if (f < 0) {
            f = 0f;
        } else if (f > 5f) {
            f = 5f;
        }
        score.setRating(f);
    }
}
