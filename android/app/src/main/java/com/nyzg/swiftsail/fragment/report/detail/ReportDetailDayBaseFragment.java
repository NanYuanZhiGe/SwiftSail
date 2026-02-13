package com.nyzg.swiftsail.fragment.report.detail;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.nyzg.swiftsail.R;

import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * 这个东西的作用就是封装了一个DateSelector
 */
abstract public class ReportDetailDayBaseFragment extends Fragment {
    final protected static String LAYOUT_KEY = "layout_key";
    private int layoutResource;
    private MyViewModel viewModel;

    @Override
    final public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = this.getArguments();
        assert bundle != null;
        this.layoutResource = bundle.getInt(LAYOUT_KEY);
        if (getParentFragment() == null) {
            viewModel = new ViewModelProvider(this).get(MyViewModel.class);
        } else {
            viewModel = new ViewModelProvider(requireParentFragment()).get(MyViewModel.class);
        }
    }

    @Nullable
    @Override
    final public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(layoutResource, container, false);
        init(father);
        if (hasDateSelector() && getDateSelectorCallback() != null) {
            this.setOnDateSelectorClicked(father.findViewById(R.id.dateSelectorContainer), getDateSelectorCallback());
        }
        return father;
    }

    /**
     * 这个函数在onCreateView中调用
     *
     * @param father onCreateView中创建的fragment
     */
    protected abstract void init(View father);


    /**
     * layout 里面是否有DateSelector，要求执行：
     * TextView dateSelector = dateSelectorContainer.findViewById(R.id.dateSelector);
     * 不报错
     */
    protected abstract boolean hasDateSelector();

    /**
     * 回调函数，同步执行，LocalDate是用户选择好的日期
     */
    protected Consumer<LocalDate> getDateSelectorCallback() {
        return null;
    }

    /**
     * 由onCreateView调用，调用的时候会自动初始化一次
     */
    @SuppressLint("DefaultLocale")
    private void setOnDateSelectorClicked(View dateSelectorContainer, Consumer<LocalDate> onDateSelected) {
        TextView dateSelector = dateSelectorContainer.findViewById(R.id.dateSelector);
        dateSelectorContainer.setOnClickListener(view -> {
            LocalDate localDate = viewModel.selectedDay > 0L ? LocalDate.ofEpochDay(viewModel.selectedDay) : LocalDate.now();
            DatePickerDialog dialog = new DatePickerDialog(requireContext(), (datePicker, year, month, day) -> {
                //用户选好之后就后台执行统计逻辑
                LocalDate selectedDate = LocalDate.of(year, month + 1, day);
                if (selectedDate.equals(localDate)) {
                    dateSelector.setText("今天");
                    return;
                } else if (selectedDate.getMonthValue() == localDate.getMonthValue()) {
                    dateSelector.setText(String.format("%d号", selectedDate.getDayOfMonth()));
                } else if (selectedDate.getYear() == localDate.getYear()) {
                    dateSelector.setText(String.format("%d月%d日", selectedDate.getMonthValue(), selectedDate.getDayOfMonth()));
                } else {
                    dateSelector.setText(String.format("%d年%d月%d日", selectedDate.getYear(), selectedDate.getMonthValue(), selectedDate.getDayOfMonth()));
                }
                viewModel.selectedDay = selectedDate.toEpochDay();
                onDateSelected.accept(selectedDate);
            }, localDate.getYear(), localDate.getMonthValue() - 1, localDate.getDayOfMonth());
            dialog.show();
        });
    }

    public static class MyViewModel extends ViewModel {
        public long selectedDay = -1L;
    }
}
