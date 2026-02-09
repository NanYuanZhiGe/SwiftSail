package com.nyzg.swiftsail.view;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.util.function.Consumer;

public class DateSelector extends androidx.appcompat.widget.AppCompatTextView {
    private long date;
    private Consumer<Long> onDateSelectedFunc;

    public DateSelector(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.date = LocalDate.now().toEpochDay();
        this.setOnClickListener(this::onClicked);
        this.setText("今天");
    }

    public void setOnDateSelectedFunc(Consumer<Long> onDateSelectedFunc) {
        this.onDateSelectedFunc = onDateSelectedFunc;
    }

    @SuppressLint("DefaultLocale")
    protected void onClicked(View v) {
        LocalDate lastDate = LocalDate.ofEpochDay(this.date);
        DatePickerDialog dialog = new DatePickerDialog(this.getContext(), (datePicker, year, month, day) -> {
            //用户选好之后就后台执行统计逻辑
            LocalDate selectedDate = LocalDate.of(year, month + 1, day);
            updateText(selectedDate);
            long currentSelected = selectedDate.toEpochDay();
            this.date = currentSelected;
            if (onDateSelectedFunc == null) {
                return;
            }
            //用户选到是同一天
            onDateSelectedFunc.accept(currentSelected);
        }, lastDate.getYear(), lastDate.getMonthValue() - 1, lastDate.getDayOfMonth());
        dialog.show();
    }

    public long getDate() {
        return date;
    }

    /**
     * 会同步更新文字
     */
    public void setDate(long date) {
        this.date = date;
        updateText(LocalDate.ofEpochDay(date));
    }

    @SuppressLint("DefaultLocale")
    protected void updateText(LocalDate date) {
        LocalDate now = LocalDate.now();
        if (date.equals(now)) {
            this.setText("今天");
        } else if (date.getYear() == now.getYear()) {
            this.setText(String.format("%d月%d日", date.getMonthValue(), date.getDayOfMonth()));
        } else {
            this.setText(String.format("%d年%d月%d日", date.getYear(), date.getMonthValue(), date.getMonthValue()));
        }
    }
}
