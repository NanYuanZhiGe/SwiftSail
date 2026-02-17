package com.nyzg.swiftsail.fragment.record;

import static com.nyzg.swiftsail.fragment.record.RecordBaseFragment.Status.CLOUD_SAVE_ERROR;
import static com.nyzg.swiftsail.fragment.record.RecordBaseFragment.Status.LOCAL_SAVE_ERROR;
import static com.nyzg.swiftsail.fragment.record.RecordBaseFragment.Status.NO_DATA;
import static com.nyzg.swiftsail.fragment.record.RecordBaseFragment.Status.NO_DATA_TO_SAVE;
import static com.nyzg.swiftsail.fragment.record.RecordBaseFragment.Status.ON_CLOUD_SAVE;
import static com.nyzg.swiftsail.fragment.record.RecordBaseFragment.Status.ON_LOCAL_SAVE;
import static com.nyzg.swiftsail.fragment.record.RecordBaseFragment.Status.ON_PAUSE;
import static com.nyzg.swiftsail.fragment.record.RecordBaseFragment.Status.ON_RECORD;
import static com.nyzg.swiftsail.fragment.record.RecordBaseFragment.Status.SAVE_DONE;

import android.app.AlertDialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.DateUtils;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.bean.NetWorkBuilder;
import com.nyzg.swiftsail.bean.NetWorkHandler;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.bean.ServerURL;
import com.nyzg.swiftsail.dbobj.RecordManual;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.repository.LoginRepository;
import com.nyzg.swiftsail.repository.RecordRecordRepository;
import com.nyzg.swiftsail.view.BackWardLayout;
import com.nyzg.swiftsail.view.record.StageLayout;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public abstract class RecordBaseFragment extends Fragment {
    public enum Status {
        //可启动集合
        NO_DATA(""), NO_DATA_TO_SAVE("（没有数据需要保存）"), SAVE_DONE("（数据保存成功）"),
        //在记录集合
        ON_RECORD("（记录中）"),
        //在暂停集合
        ON_PAUSE("（暂停中）"), ON_LOCAL_SAVE("（数据保存中）"), ON_CLOUD_SAVE("（上传云端中）"), LOCAL_SAVE_ERROR("（数据保存失败）"), CLOUD_SAVE_ERROR("云端保存失败");

        Status(String name) {
            mName = name;
        }

        private final String mName;

        public String getName() {
            return mName;
        }
    }

    public static class MyViewModel extends ViewModel {
        @Nullable
        LocalDateTime mStartTime;
        @Nullable
        LocalDateTime mEndTime;
        long mExposeValue;
        @Nullable
        String mDetailValue;
        final MutableLiveData<Drawable> submitDrawable = new MutableLiveData<>();
        final MutableLiveData<Status> mStatus = new MutableLiveData<>(NO_DATA);
    }

    protected MyViewModel mViewModel;
    private StageLayout stageLayout;
    private BackWardLayout backWardLayout;
    private OnBackPressedCallback callback;

    @Override
    final public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(MyViewModel.class);
        myOnCreated(savedInstanceState);
    }

    @Override
    final public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        stageLayout = view.findViewById(R.id.stageLayout);
        stageLayout.setMCancel(this::myOnCancel);
        stageLayout.setMStart(this::myOnStart);
        stageLayout.setMStop(this::myOnStop);
        stageLayout.setMSubmit(this::myOnSubmit);

        mViewModel.submitDrawable.observe(getViewLifecycleOwner(), drawable -> {
            if (drawable == null) {
                return;
            }
            stageLayout.setSubmitDrawable(drawable);
        });

        backWardLayout = view.findViewById(R.id.backward);
        if (RecordRecordRepository.getInstance().SELECT_NAME.getValue() == null) {
            backWardLayout.setTitlePrefix("运动");
        } else {
            backWardLayout.setTitlePrefix(RecordRecordRepository.getInstance().SELECT_NAME.getValue());
        }
        //更新标题的UI，告诉用户当前的记录状态
        mViewModel.mStatus.observe(getViewLifecycleOwner(), status -> {
            if (status == null) {
                return;
            }
            backWardLayout.setTitleStatus(status.getName());
        });
        Runnable onQuitRun = () -> {
            assert mViewModel.mStatus.getValue() != null;
            switch (mViewModel.mStatus.getValue()) {
                case NO_DATA, SAVE_DONE, NO_DATA_TO_SAVE:
                    requireActivity().getSupportFragmentManager().popBackStack();
                    break;
                case ON_RECORD:
                    myOnStop();
                case ON_PAUSE:
                    new AlertDialog.Builder(requireContext())
                            .setTitle("")
                            .setMessage("您本次的运动记录未保存，退出前要保存吗？")
                            .setPositiveButton("保存并上传云端", (d, i) -> saveRecord(true))
                            .setNeutralButton("仅保存到本地", (d, i) -> saveRecord(false))
                            .setNegativeButton("不保存", (d, i) -> requireActivity().getSupportFragmentManager().popBackStack())
                            .create()
                            .show();
                    break;
                case LOCAL_SAVE_ERROR:
                    new AlertDialog.Builder(requireContext())
                            .setTitle("")
                            .setMessage("您本地的数据保存失败，是否需要重新尝试保存？")
                            .setPositiveButton("重新保存", (d, i) -> saveRecord(true))
                            .setNegativeButton("取消", (d, i) -> requireActivity().getSupportFragmentManager().popBackStack())
                            .create()
                            .show();
                    break;
                case CLOUD_SAVE_ERROR:
                    new AlertDialog.Builder(requireContext())
                            .setTitle("")
                            .setMessage("您的数据上传云端失败，是否需要重新上传？")
                            .setPositiveButton("重新上传", (d, i) -> saveRecord(true))
                            .setNegativeButton("取消", (d, i) -> requireActivity().getSupportFragmentManager().popBackStack())
                            .create()
                            .show();
                    break;
                case ON_LOCAL_SAVE, ON_CLOUD_SAVE:
                    new AlertDialog.Builder(requireContext())
                            .setTitle("")
                            .setMessage("您正在保存数据中，退出后如果数据保存失败将无法恢复！")
                            .setPositiveButton("退出", (d, i) -> requireActivity().getSupportFragmentManager().popBackStack())
                            .setNegativeButton("取消", null)
                            .create()
                            .show();
                default:
                    requireActivity().getSupportFragmentManager().popBackStack();
            }
        };
        backWardLayout.setOnClickListener(v -> onQuitRun.run());
        callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onQuitRun.run();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(callback);
        myOnViewCreated(view);
    }

    private void myOnSubmit() {
        assert mViewModel.mStatus.getValue() != null;
        if (mViewModel.mStartTime == null) {
            return;
        }
        mViewModel.mEndTime = LocalDateTime.now();
        assert mViewModel.mEndTime != null;
        switch (mViewModel.mStatus.getValue()) {
            case NO_DATA, SAVE_DONE, NO_DATA_TO_SAVE://不需要提交数据
            case ON_LOCAL_SAVE, ON_CLOUD_SAVE://已经在保存数据了
                return;
            case ON_RECORD:
                myOnStop();
            case ON_PAUSE, LOCAL_SAVE_ERROR, CLOUD_SAVE_ERROR:
                new AlertDialog.Builder(requireContext())
                        .setTitle("")
                        .setTitle(String.format("您需要保存从%s 至 %s 的数据吗？",
                                mViewModel.mStartTime.format(DateUtils.HH_mm_ss), mViewModel.mEndTime.format(DateUtils.HH_mm_ss)))
                        .setPositiveButton("保存并上传至云端", (d, i) -> {
                            summary();
                            saveRecord(true);
                        })
                        .setNeutralButton("仅保存至本地", (d, i) -> {
                            summary();
                            saveRecord(false);
                        })
                        .setNegativeButton("取消", null);
        }
    }

    private void myOnStop() {
        assert mViewModel.mStatus.getValue() != null;
        switch (mViewModel.mStatus.getValue()) {
            case NO_DATA, SAVE_DONE, NO_DATA_TO_SAVE://不需要暂停
                return;
            case ON_RECORD:
                doStop();
                mViewModel.mStatus.setValue(ON_PAUSE);
                break;
            default://已经属于暂停的状态了
        }
    }

    private void myOnStart() {
        assert mViewModel.mStatus.getValue() != null;
        switch (mViewModel.mStatus.getValue()) {
            case ON_CLOUD_SAVE, ON_LOCAL_SAVE:
                new AlertDialog.Builder(requireContext())
                        .setTitle("")
                        .setMessage("您正在保存数据，继续记录可能导致保存失败丢失数据！")
                        .setPositiveButton("继续记录", (d, i) -> {
                            if(doStart()){
                                mViewModel.mStatus.setValue(ON_RECORD);
                            }else{
                                stageLayout.switchToPause();
                                mViewModel.mStatus.setValue(ON_PAUSE);
                                doReset();
                                mViewModel.mStatus.setValue(NO_DATA);
                            }
                        })
                        .setNegativeButton("取消", null)
                        .create()
                        .show();
                break;
            case LOCAL_SAVE_ERROR, CLOUD_SAVE_ERROR:
                new AlertDialog.Builder(requireContext())
                        .setTitle("")
                        .setMessage("您先前保存数据没有成功，要重新保存吗？")
                        .setPositiveButton("保存并上传云端", (d, i) -> saveRecord(true))
                        .setNeutralButton("仅保存本地", (d, i) -> saveRecord(false))
                        .setNegativeButton("不保存", (d, i) -> {
                            if(doStart()){
                                mViewModel.mStatus.setValue(ON_RECORD);
                            }else{
                                stageLayout.switchToPause();
                                mViewModel.mStatus.setValue(ON_PAUSE);
                                doReset();
                                mViewModel.mStatus.setValue(NO_DATA);
                            }
                        })
                        .create()
                        .show();
                break;
            case ON_PAUSE://普通的暂停可以恢复
            case NO_DATA, SAVE_DONE, NO_DATA_TO_SAVE:
                if(doStart()){
                    mViewModel.mStatus.setValue(ON_RECORD);
                }else{
                    stageLayout.switchToPause();
                    mViewModel.mStatus.setValue(ON_PAUSE);
                    doReset();
                    mViewModel.mStatus.setValue(NO_DATA);
                }
                break;
            case ON_RECORD://已经处于在记录的状态了
                break;
        }
    }

    private void myOnCancel() {
        assert mViewModel.mStatus.getValue() != null;
        switch (mViewModel.mStatus.getValue()) {
            case ON_PAUSE, LOCAL_SAVE_ERROR, CLOUD_SAVE_ERROR:
            case NO_DATA, SAVE_DONE, NO_DATA_TO_SAVE://不需要暂停
                doReset();
                mViewModel.mStatus.setValue(NO_DATA);
                return;
            case ON_RECORD:
                doStop();
                mViewModel.mStatus.setValue(ON_PAUSE);
                new AlertDialog.Builder(requireContext())
                        .setTitle("")
                        .setMessage("您确定要重新记录吗？")
                        .setPositiveButton("确定", (d, i) -> {
                            doReset();
                            mViewModel.mStatus.setValue(NO_DATA);
                        })
                        .setNegativeButton("取消", null)
                        .create()
                        .show();
                break;
            case ON_CLOUD_SAVE, ON_LOCAL_SAVE:
                new AlertDialog.Builder(requireContext())
                        .setTitle("")
                        .setMessage("您正在保存数据中，此操作会导致数据清除，如果保存失败，无法恢复数据！")
                        .setPositiveButton("清除数据", (d, i) -> {
                            doReset();
                            mViewModel.mStatus.setValue(NO_DATA);
                        })
                        .setNegativeButton("取消", null)
                        .create()
                        .show();
        }
    }

    protected abstract void doStop();

    protected abstract boolean doStart();

    private void doReset() {
        reset();
        mViewModel.mExposeValue = 0L;
        mViewModel.mDetailValue = null;
        mViewModel.mStartTime = null;
        mViewModel.mEndTime = null;
    }

    protected abstract void reset();

    protected abstract void summary();

    @SuppressWarnings(value = {"unused"})
    protected void myOnViewCreated(@NonNull View father) {
    }

    protected void myOnCreated(@Nullable Bundle bundle) {
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        callback.remove();
    }

    /**
     * 保证status==Pause
     * 函数执行完后status=SAVE_DONE
     */
    private void saveRecord(boolean uploadCloud) {
        mViewModel.mStatus.setValue(ON_LOCAL_SAVE);
        //如果没有数据需要保存
        if (mViewModel.mStartTime == null
                || mViewModel.mDetailValue == null) {
            mViewModel.mStatus.setValue(NO_DATA_TO_SAVE);
            return;
        }
        //先进行本地保存
        User user = LoginRepository.getInstance().currentUser.getValue();
        if (user == null) {
            user = GlobalInstance.LOCAL_USER;
        }
        final long userId = user.id;
        final String uuid = UUID.randomUUID().toString();
        final LocalDate date = mViewModel.mStartTime.toLocalDate();
        final long exposeValue = mViewModel.mExposeValue;
        final String detailValue = mViewModel.mDetailValue;
        if (mViewModel.mEndTime == null) {
            mViewModel.mEndTime = LocalDateTime.now();
        }
        assert mViewModel.mEndTime != null;
        final RecordManual record = new RecordManual(
                (short) 0, getType(), user.id, uuid,
                exposeValue, detailValue,
                mViewModel.mStartTime.toEpochSecond(DateUtils.ZONE_OFFSET), mViewModel.mEndTime.toEpochSecond(DateUtils.ZONE_OFFSET),
                date.toEpochDay(), DateUtils.getEpochWeek(date.toEpochDay()), DateUtils.getEpochMonth(date), DateUtils.getEpochYear(date)
        );
        CompletableFuture.runAsync(() -> {
            try {
                mViewModel.submitDrawable.postValue(ContextCompat.getDrawable(GlobalApplication.getAppContext(), R.drawable.download));
                SQLiteDB.getDatabase(GlobalApplication.getAppContext())
                        .recordManualTable().insertRecordManual(record);
                //如果需要云端保存
                if (userId != GlobalInstance.LOCAL_USER.id && uploadCloud) {
                    mViewModel.mStatus.postValue(ON_CLOUD_SAVE);
                    mViewModel.submitDrawable.postValue(ContextCompat.getDrawable(GlobalApplication.getAppContext(), R.drawable.cloud_upload));
                    NetWorkHandler.handleNetRespAfterLogin(
                            null,
                            NetWorkBuilder.doChunkRequest(NetWorkBuilder.buildJsonRequestJwt(
                                    ServerURL.URL_SYNC_BACKUP_MANUAL, ServerURL.POST, record
                            )),
                            errMsg -> {//不用理
                                mViewModel.mStatus.postValue(CLOUD_SAVE_ERROR);
                            },
                            resp -> SQLiteDB.getDatabase(GlobalApplication.getAppContext()).recordManualTable().updateSyncHasDone(userId, getType(), uuid),
                            Void.class
                    );
                }
            } catch (Exception ignore) {
                mViewModel.mStatus.postValue(LOCAL_SAVE_ERROR);
            }
            mViewModel.submitDrawable.postValue(ContextCompat.getDrawable(GlobalApplication.getAppContext(), R.drawable.check));
            mViewModel.mStatus.postValue(SAVE_DONE);
        });
    }

    private @NonNull String getType() {
        if (RecordRecordRepository.getInstance().SELECT_TYPE.getValue() == null) {
            return "运动";
        }
        return RecordRecordRepository.getInstance().SELECT_TYPE.getValue();
    }
}