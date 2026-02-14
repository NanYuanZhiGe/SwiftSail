package com.nyzg.swiftsail.fragment.report.detail;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.MyJsonSerializer;
import com.nyzg.swiftsail.bean.RecordType;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.bean.ThreadPool;
import com.nyzg.swiftsail.dao.RecordTable;
import com.nyzg.swiftsail.dbobj.Record;
import com.nyzg.swiftsail.obj.MyStep;
import com.nyzg.swiftsail.repository.LoginRepository;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ReportDetailStepDayFragment extends ReportDetailDayBaseFragment {
    public static Fragment getInstance(int layoutResource) {
        Fragment fragment = new ReportDetailStepDayFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(LAYOUT_KEY, layoutResource);
        fragment.setArguments(bundle);
        return fragment;
    }

    private final MutableLiveData<Integer> stepCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> floorCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> seatCount = new MutableLiveData<>(0);

    private TextView step;
    private TextView stepDesc;
    private TextView floor;
    private TextView floorDesc;
    private TextView seat;
    private TextView seatDesc;

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    protected void init(View father) {
        step = father.findViewById(R.id.step);
        stepDesc = father.findViewById(R.id.stepDesc);
        stepCount.observe(getViewLifecycleOwner(), val -> {
            if (val == null) {
                return;
            }
            step.setText(val + "");
            stepDesc.setText(StepIndicator.getStepDesc(val));
        });
        floor = father.findViewById(R.id.floor);
        floorDesc = father.findViewById(R.id.floorDesc);
        floorCount.observe(getViewLifecycleOwner(), val -> {
            if (val == null) {
                return;
            }
            floor.setText(val + "");
            floorDesc.setText(StepIndicator.getFloorDesc(val));
        });
        seat = father.findViewById(R.id.seat);
        seatDesc = father.findViewById(R.id.seatDesc);
        seatCount.observe(getViewLifecycleOwner(), val -> {
            if (val == null) {
                return;
            }
            int hour = val / 60;
            int minute = val % 60;
            String hourTxt = "";
            SpannableString spannableString;
            if (hour > 0) {
                hourTxt = hour + "";
                spannableString = new SpannableString(String.format("%s小时%02d分钟", hourTxt, minute));
            } else {
                spannableString = new SpannableString(String.format("%d分钟", minute));
            }
            float smallSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, //sp
                    getResources().getDisplayMetrics());
            int len = spannableString.length();
            if (hour > 0) {
                spannableString.setSpan(new AbsoluteSizeSpan((int) smallSizePx), hourTxt.length(), hourTxt.length() + 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }
            spannableString.setSpan(new AbsoluteSizeSpan((int) smallSizePx), len - 2, len, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            seat.setText(spannableString);
            seatDesc.setText(StepIndicator.getSeatDesc(val));
        });
    }

    @Override
    protected boolean hasDateSelector() {
        return true;
    }

    @Override
    protected Consumer<LocalDate> getDateSelectorCallback() {
        return localDate -> CompletableFuture.supplyAsync(() -> {
            if (LoginRepository.getInstance().currentUser.getValue() == null) {
                reset();
                return null;
            }
            try {
                RecordTable recordTable = SQLiteDB.getDatabase(GlobalApplication.getAppContext()).recordTable();
                Record record = recordTable.getSpecificationRecord(LoginRepository.getInstance().currentUser.getValue().id,
                        RecordType.STEP, localDate.toEpochDay());
                if (record == null || record.detailValue == null) {
                    reset();
                    return null;
                }
                MyStep myStep = MyJsonSerializer.deSerialize(record.detailValue, MyStep.class);
                stepCount.postValue(myStep.steps);
                floorCount.postValue(myStep.floors);
                seatCount.postValue(myStep.sedentaryMinutes);
            } catch (Exception ignore) {
                reset();
            }
            return null;
        }, ThreadPool.QUICK_CHANGE_THREAD_POOL);
    }

    private void reset() {
        seatCount.postValue(0);
        floorCount.postValue(0);
        seatCount.postValue(0);
    }

    private static class StepIndicator {
        public static String getStepDesc(int val) {
            if (val == 0) return "";
            if (val < 300) return "今天还没开始动起来哦～";
            if (val < 1000) return "迈出第一步，就是进步！";
            if (val < 3000) return "继续走，活力正在积累中！";
            if (val < 6000) return "不错！已经完成日常基础目标啦！";
            if (val < 10000) return "接近万步目标，再加把劲！";
            if (val < 15000) return "太棒了！你今天走了超多步，活力满满！";
            return "您是不是该休息一会？";
        }

        public static String getFloorDesc(int val) {
            if (val == 0) return "";
            if (val < 7) return "今天还没爬楼呢，试试走楼梯吧！";
            if (val < 15) return "爬了一层，小进步也是进步！";
            if (val < 5 * 15) return "坚持爬楼，腿部力量会越来越强！";
            if (val < 10 * 15) return "今天爬了这么多层，心肺功能在悄悄变好！";
            if (val < 20 * 15) return "你是楼梯王者！省电梯还锻炼身体～";
            return "登高望远，你的毅力让人佩服！";
        }

        public static String getSeatDesc(int val) {
            if (val == 0) return "";
            if (val < 90) return "完美！今天几乎没久坐，继续保持！";
            if (val < 120) return "久坐时间很短，你的身体会感谢你！";
            if (val < 200) return "偶尔休息一下，记得起来走动哦～";
            if (val < 300) return "建议每小时起身活动2分钟，缓解疲劳！";
            if (val < 500) return "久坐稍长，不妨站起来伸个懒腰？";
            return "你又加班加点对不？加油！";
        }
    }
}
