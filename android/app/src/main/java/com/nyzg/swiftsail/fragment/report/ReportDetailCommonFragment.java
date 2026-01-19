package com.nyzg.swiftsail.fragment.report;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.fragment.BackPressQuitFragment;
import com.nyzg.swiftsail.obj.Pair;
import com.nyzg.swiftsail.obj.SumType;
import com.nyzg.swiftsail.view.RoundedBarChart;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 这个类支持为不同类型的健康提供一个统一的视图，减少重复代码，方便管理
 * 为了使用这个类，你需要提供下面三个参数：
 * themeColor，柱状图的主题色
 * funcKey，逻辑函数在哈希表中的键
 * logicFunc，逻辑函数
 * sumType，统计类型
 * avgFormatter 用于格式化平均值
 * 这个类对应的fragment被销毁后，会自动清理哈希表，避免内存泄漏
 * 这个逻辑在类里面是同步调用的，如果你需要进行异步操作，请注意
 */
public class ReportDetailCommonFragment extends BackPressQuitFragment {
    /*
    我的代码保证下面：
    1. 每一个逻辑函数的key都是唯一的，所以插入和删除hash map的时候绝对不会发生冲突（逻辑上）
    所以不用担心内存泄漏、访问冲突、空指针的问题
     */
    private static final Map<String, Pair<SumType, Function<FuncParam, Pair<Long, List<BarEntry>>>>> FUNC_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Function<Float, String>> FUNC_AVG_FORMATTER = new ConcurrentHashMap<>();
    /*
    这个线程池被设计为静态的目的是防止用户多次提交任务导致大量任务堆积，
    同时保证用户在频繁切换UI+提交的时候保证数据的最新。
    因为线程数很少——不需要很多线程，UI的设计决定用户的页面不会给用户太多选择，
    然后任务队列很小，如果用户大量提交新的请求，就会不断丢掉不要的旧请求，
    从而尽可能保证数据的最新
     */
    private static final ExecutorService THREAD_POOL = new ThreadPoolExecutor(2, 2, 5 * 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1), new ThreadFactory() {
        private final AtomicInteger count = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName("ReportDetailCalculateThread-" + count.getAndAdd(1));
            return thread;
        }
    }, new ThreadPoolExecutor.DiscardOldestPolicy());
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy年M月d日");
    private static final String[] WEEK_X_LABEL = new String[]{"一", "二", "三", "四", "五", "六", "日"};
    private static final String[] MONTH_X_LABEL = new String[]{"第一周", "第二周", "第三周", "第四周", "第五周", "第六周", "第七周", "第八周"};
    private static final String[] YEAR_X_LABEL = new String[]{"1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"};
    private static final String THEME_COLOR_KEY = "color_key";
    private static final String SYNC_FUNC_KEY = "sync_key";
    private Function<Float, String> avgFormatter;
    private MyViewModel viewModel;
    private int themeColor;
    private String funcKey;
    private SumType sumType;
    private TextView textView;
    private RoundedBarChart barChart;
    private BarDataSet barDataSet = null;
    private LinearLayout linearLayout;

    /**
     * 逻辑函数要求同步执行，内部的调用时异步的
     * 逻辑函数的返回值时有要求，必须按照时间顺序从左到右（从远到近）排列
     *
     * @param themeColor       柱状图的颜色
     * @param logicFunc        逻辑函数，主要是从数据库中进行查找和统计和操作
     * @param funcKey          逻辑函数在hashmap中的key
     * @param sumType          统计类型，比如按周统计、按月统计，按年统计，按所有数据统计
     * @param averageFormatter 对于每个BarEntry中的数据，怎么转化为字符串，比如9.20是9小时12分钟
     * @return 返回这个类的一个新的实例
     */
    public static Fragment getInstance(int themeColor, Function<FuncParam, Pair<Long, List<BarEntry>>> logicFunc, String funcKey, SumType sumType, Function<Float, String> averageFormatter) {
        Fragment fragment = new ReportDetailCommonFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(THEME_COLOR_KEY, themeColor);
        bundle.putString(SYNC_FUNC_KEY, funcKey);
        FUNC_CACHE.put(funcKey, new Pair<>(sumType, logicFunc));
        FUNC_AVG_FORMATTER.put(funcKey, averageFormatter);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = this.getArguments();
        assert bundle != null;
        themeColor = bundle.getInt(THEME_COLOR_KEY);
        funcKey = bundle.getString(SYNC_FUNC_KEY);
        Pair<SumType, Function<FuncParam, Pair<Long, List<BarEntry>>>> pair = FUNC_CACHE.get(funcKey);
        assert pair != null;
        sumType = pair.getA();
        viewModel = new ViewModelProvider(this).get(MyViewModel.class);
        avgFormatter = FUNC_AVG_FORMATTER.get(funcKey);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_report_common_summary, container, false);
        textView = father.findViewById(R.id.avgTxt);
        barChart = father.findViewById(R.id.barChart);
        View view = father.findViewById(R.id.dateSelectorContainer);
        initDateSelectorContainer(view, sumType);
        linearLayout = father.findViewById(R.id.avgList);
        linearLayout.setNestedScrollingEnabled(false);
        TextView listName = father.findViewById(R.id.listName);
        if (sumType == SumType.WEEK) {
            listName.setText("每天数据");
        } else if (sumType == SumType.MONTH) {
            listName.setText("每周平均值");
        } else if (sumType == SumType.YEAR) {
            listName.setText("每月平均值");
        } else {
            listName.setText("每年平均值");
        }
        return father;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //执行第一次进入到这个Fragment的时候初始化函数
        doLogicFunc(getFuncParam(LocalDate.now(), sumType));
        viewModel.funcResult.observe(getViewLifecycleOwner(), pair -> {
            /*
            entry中的数据如下：
            1. sumType==Week，那么entry中就是每天的数据，offset为epochDay
            2. sumType==Month，那么entry中就是每周的平均数据，offset为epochDay
            3. sumType==Year，那么entry中就是每月的平均数据，offset为epochDay
            4. sumType=Total，那么entry中就是每年的平均数据，offset为起始年
             */
            List<BarEntry> entries = pair.getB();
            long offset = pair.getA();
            notifyTextViewChange((float) entries.stream().mapToDouble(BarEntry::getY).average().orElse(0L));
            notifyRecyclerViewDatasetChange(offset, entries);
            notifyBarChartDatasetChange(offset, entries);
        });
    }

    private void notifyBarChartDatasetChange(long offset, List<BarEntry> entries) {
        if (this.barDataSet != null) {//已经初始化过了
            //对于total，需要重新刷新一次X轴，因为offset发生了变化
            if (sumType == SumType.TOTAL) {
                XAxis xAxis = barChart.getXAxis();
                xAxis.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getAxisLabel(float value, AxisBase axis) {
                        return String.valueOf((int) (offset + value));
                    }
                });
            }
            barDataSet.setValues(entries);
            barChart.notifyDataSetChanged();
            barChart.invalidate();
            return;
        }
        //第一次执行
        barDataSet = new BarDataSet(entries, "");
        barDataSet.setColors(ContextCompat.getColor(requireContext(), themeColor));
        float barWidth = 0.2f;
        if (sumType == SumType.YEAR) {
            barWidth = 0.1f;//12个月
        }
        BarData barData = new BarData(barDataSet);
        barData.setBarWidth(barWidth);
        barData.setDrawValues(false);
        XAxis xAxis = barChart.getXAxis();
        if (sumType == SumType.WEEK) {
            xAxis.setValueFormatter(new IndexAxisValueFormatter(WEEK_X_LABEL));
        } else if (sumType == SumType.MONTH) {
            xAxis.setValueFormatter(new IndexAxisValueFormatter(MONTH_X_LABEL));
        } else if (sumType == SumType.YEAR) {
            xAxis.setValueFormatter(new IndexAxisValueFormatter(YEAR_X_LABEL));
        } else if (sumType == SumType.TOTAL) {
            xAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getAxisLabel(float value, AxisBase axis) {
                    return String.valueOf((int) (offset + value));
                }
            });
        }
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        Log.v("myTag",entries.toString());

        barChart.getAxisLeft().setEnabled(false);
        YAxis yAxis = barChart.getAxisRight();
        yAxis.setDrawAxisLine(false);
        yAxis.setDrawGridLines(false);
        LimitLine limitLine=new LimitLine((float)entries.stream().mapToDouble(BarEntry::getY).average().orElse(0),"");
        limitLine.setLineWidth(3f);
        limitLine.setLineColor(themeColor);
        yAxis.addLimitLine(limitLine);

        barChart.setDescription(null);
        barChart.setTouchEnabled(false);
        barChart.setData(barData);
        barChart.setFitBars(true);
        barChart.invalidate();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void notifyRecyclerViewDatasetChange(long offset, List<BarEntry> entries) {
        if (entries == null) {//以防entries为空导致出错
            return;
        }
        linearLayout.removeAllViews();
        for (int i = 0; i < entries.size(); ++i) {
            View view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_report_detail_sum, linearLayout, false);
            setDetailViewHolder(view, i, entries, offset);
            linearLayout.addView(view);
        }
    }

    private void notifyTextViewChange(Float avg) {
        String avgText = avgFormatter.apply(avg);
        String prefix = "平均";
        SpannableString spannable = new SpannableString(prefix + avgText);
        int start = prefix.length();
        int end = spannable.length();
        float largeSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 32, //sp
                getResources().getDisplayMetrics());
        spannable.setSpan(new AbsoluteSizeSpan((int) largeSizePx), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(spannable);
    }

    private void doLogicFunc(FuncParam funcParam) {
        //如果用户提交了请求后快速地退出fragment或者切换到其他fragment
        //导致这个fragment被销毁，就会执行destroy，然后这个就不会执行
        //当然，如果这个先提前执行，然后用户进行操作，导致fragment被销毁了也没有关系
        //这里使用了viewModel，保证UI的更新安全
        FUNC_CACHE.computeIfPresent(funcKey, (k, v) -> {
            Function<FuncParam, Pair<Long, List<BarEntry>>> function = v.getB();
            CompletableFuture.supplyAsync(() -> function.apply(funcParam), THREAD_POOL).thenAcceptAsync(barEntryResult -> viewModel.funcResult.setValue(barEntryResult), ContextCompat.getMainExecutor(requireContext()));
            return null;
        });
    }

    /**
     * 对于DateSelector，不论怎么样，点击的时候都是选择一个日期，通过计算日期，从而确定
     * 这个是第几周，第几月、第几年
     */
    private void initDateSelectorContainer(View view, SumType sumType) {
        //popup一个窗口，让用户选择日期
        view.setOnClickListener(v -> {
            LocalDate localDate = LocalDate.now();
            DatePickerDialog dialog = new DatePickerDialog(requireContext(), (datePicker, year, month, day) -> {
                //用户选好之后就后台执行统计逻辑
                LocalDate selectedDate = LocalDate.of(year, month + 1, day);
                doLogicFunc(getFuncParam(selectedDate, sumType));
            }, localDate.getYear(), localDate.getMonthValue() - 1, localDate.getDayOfMonth());
            dialog.show();
        });
    }

    private FuncParam getFuncParam(LocalDate date, SumType sumType) {
        FuncParam funcParam = new FuncParam();
        //全部包含最后一天
        if (sumType == SumType.WEEK) {//确定这一天的周开始和周结尾
            LocalDate startWeek = date.with(DayOfWeek.MONDAY);
            LocalDate endWeek = date.with(DayOfWeek.SUNDAY);
            funcParam.fromDay = startWeek.toEpochDay();
            funcParam.endDay = endWeek.toEpochDay();
        } else if (sumType == SumType.MONTH) {//确定这一天的月开始和月结束
            LocalDate startOfMonth = date.withDayOfMonth(1);
            LocalDate endOfMonth = date.with(TemporalAdjusters.lastDayOfMonth());
            funcParam.fromDay = startOfMonth.toEpochDay();
            funcParam.endDay = endOfMonth.toEpochDay();
        } else if (sumType == SumType.YEAR) {//确定这一天的年开始和年结束
            LocalDate startOfYear = date.withDayOfYear(1);
            LocalDate endOfYear = date.with(TemporalAdjusters.lastDayOfYear());
            funcParam.fromDay = startOfYear.toEpochDay();
            funcParam.endDay = endOfYear.toEpochDay();
        }
        funcParam.sumType = sumType;
        return funcParam;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        FUNC_CACHE.remove(funcKey);
        FUNC_AVG_FORMATTER.remove(funcKey);
    }

    @SuppressLint("DefaultLocale")
    private void setDetailViewHolder(View itemView, int position, List<BarEntry> barEntries, long offset) {
        TextView indexView = itemView.findViewById(R.id.index);
        TextView detailView = itemView.findViewById(R.id.detail);
        TextView dateView = itemView.findViewById(R.id.date);
        //这里需要注意一点，barEntry中的data它们的顺序是时间顺序，比如：
        //周一，周二，周三
        //但是我们希望离用户近的那一天在上面，所以我们需要倒序排列
        int index = barEntries.size() - 1 - position;
        if (sumType == SumType.WEEK) {
            indexView.setText(String.format("周%s", WEEK_X_LABEL[index]));
        } else if (sumType == SumType.MONTH) {
            indexView.setText(MONTH_X_LABEL[index]);
        } else if (sumType == SumType.YEAR) {
            indexView.setText(YEAR_X_LABEL[index]);
        } else if (sumType == SumType.TOTAL) {
            indexView.setText(String.format("%d年", offset + index));
        }
        //平均数（加上单位），比如9小时2分钟，3600（步数）之类的
        detailView.setText(avgFormatter.apply(barEntries.get(index).getY()));
        //日期范围
        //对于Week、Month、Year来说，offset就是barEntry位置等于0的时候的epochDay
        //对于Total来说，offset就是barEntry等于0的时候的那一年
        //由于我们是逆序来排布的，所以加的是index而不是position
        if (sumType == SumType.WEEK) {
            long date = offset + index;
            dateView.setText(LocalDate.ofEpochDay(date).format(DATE_TIME_FORMATTER));
        } else if (sumType == SumType.MONTH) {
            //需要计算第几周，注意逆序
            LocalDate offsetWeek = LocalDate.ofEpochDay(offset);
            LocalDate toMonday = offsetWeek.with(DayOfWeek.MONDAY);
            long startDay = toMonday.toEpochDay();
            long passWeek = index * 7L;//相比于offset，加了position*7天的时间
            long weekStart = startDay + passWeek;
            //weekStart是对齐过后的，如果barEntry中只有一周的数据，而起始位置offset不是周一
            //这里就会有问题，所以需要进行一些细化的判断
            if (weekStart < offset) {
                weekStart = offset;
            }
            //barEntry中的最后一个可能是不完整的一周
            long endOfMonth = offsetWeek.with(TemporalAdjusters.lastDayOfMonth()).toEpochDay();
            long weekEnd = startDay + passWeek + 6;
            long weekNow = LocalDate.now().toEpochDay();
            if (weekEnd > weekNow || weekEnd > endOfMonth) {
                weekEnd = Math.min(weekNow, endOfMonth);
            }
            dateView.setText(String.format("%s-%s", LocalDate.ofEpochDay(weekStart).format(DATE_TIME_FORMATTER), LocalDate.ofEpochDay(weekEnd).format(DATE_TIME_FORMATTER)));
        } else if (sumType == SumType.YEAR) {
            //需要计算第几月，注意逆序
            //相比于offset，增加了position个月
            LocalDate startMonth = LocalDate.ofEpochDay(offset).plusMonths(position);
            //获取这个月的结束，如果这个月的结束大于当前时间，说明这个月没有过完，取当前时间
            long monthEnd = startMonth.with(TemporalAdjusters.lastDayOfMonth()).toEpochDay();
            long monthNow = LocalDate.now().toEpochDay();
            if (monthEnd > monthNow) {
                monthEnd = monthNow;
            }
            dateView.setText(String.format("%s-%s", startMonth.format(DATE_TIME_FORMATTER), LocalDate.ofEpochDay(monthEnd).format(DATE_TIME_FORMATTER)));
        } else if (sumType == SumType.TOTAL) {
            long year = offset + index;
            dateView.setText(String.format("%s年", year));
        }
    }


    public static class MyViewModel extends ViewModel {
        public final MutableLiveData<Pair<Long, List<BarEntry>>> funcResult = new MutableLiveData<>();
    }

    public static class FuncParam {
        public long fromDay;//起始的日期，epoch day，包括在内
        public long endDay;//结束的日期，epoch day，包括在内
        public SumType sumType;//求平均的方式，天、周、月、年
    }
}