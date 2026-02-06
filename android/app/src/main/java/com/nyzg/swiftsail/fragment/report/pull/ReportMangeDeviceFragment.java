package com.nyzg.swiftsail.fragment.report.pull;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.EncryptThreadSafe;
import com.nyzg.swiftsail.bean.NetWorkBuilder;
import com.nyzg.swiftsail.bean.NetWorkHandler;
import com.nyzg.swiftsail.bean.ServerURL;
import com.nyzg.swiftsail.netobj.report.Watch;
import com.nyzg.swiftsail.fragment.InnerFragment;
import com.nyzg.swiftsail.netobj.report.WatchDeleteReq;
import com.nyzg.swiftsail.netobj.report.WatchGetResp;
import com.nyzg.swiftsail.obj.Pair;
import com.skydoves.balloon.ArrowOrientation;
import com.skydoves.balloon.ArrowPositionRules;
import com.skydoves.balloon.Balloon;
import com.skydoves.balloon.BalloonAnimation;
import com.skydoves.balloon.BalloonSizeSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ReportMangeDeviceFragment extends InnerFragment {
    public static Fragment getInstance() {
        return new ReportMangeDeviceFragment();
    }

    private RecyclerView notificationRecycler;
    private RecyclerView recyclerView;
    private ImageView notification;
    private DrawerLayout drawerLayout;
    private final List<String> notificationList = new ArrayList<>();
    private MyViewModel viewModel;
    private TextView currentRunning;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MyViewModel.class);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_report_device_mange, container, false);
        //返回按钮
        father.findViewById(R.id.backward).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        //----------------设置消息提醒相关的内容----------------------------------
        notification = father.findViewById(R.id.notification);
        notification.setOnClickListener(this::onNotificationClicked);
        drawerLayout = father.findViewById(R.id.drawerLayout);
        //用户手动划开消息栏的时候自动改变图标
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                notification.setImageDrawable(ContextCompat.getDrawable(
                        GlobalApplication.getAppContext(), R.drawable.notifications
                ));
            }
        });
        notificationRecycler = father.findViewById(R.id.msgBox);
        notificationRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        notificationRecycler.setAdapter(new ReportAddDeviceFragment.MyAdapter(notificationList));
        currentRunning = father.findViewById(R.id.currentRunning);
        //----------------设置主题内容展示相关的----------------------------------
        recyclerView = father.findViewById(R.id.devices);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(new MyAdapter(viewModel));
        return father;
    }

    /**
     * fragment创建完毕后，就自动添加数据
     */
    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //通过liveData 的方式来更新数据
        viewModel.watchList.observe(getViewLifecycleOwner(), data -> {
            MyAdapter myAdapter = (MyAdapter) recyclerView.getAdapter();
            if (myAdapter == null) {
                return;
            }
            myAdapter.setWatchList(data);
            myAdapter.notifyDataSetChanged();//这个要求全部更新
        });
        viewModel.deleteProxy.observe(getViewLifecycleOwner(), key -> {
            if (key == null) {
                return;
            }
            viewModel.deleteProxy.setValue(null);
            new AlertDialog.Builder(requireContext())
                    .setTitle("")
                    .setMessage("您确定要删除此设备吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        //首先更新UI为删除中
                        MyAdapter adapter = (MyAdapter) recyclerView.getAdapter();
                        if (adapter == null) {//小于0已经过滤了
                            return;
                        }
                        //进行网络请求删除数据
                        Watch watch = adapter.getWatchMap().getByKey(key);
                        if (watch == null) {
                            return;
                        }
                        String tempName = watch.name;
                        watch.name = watch.name + "（删除中）";
                        {
                            int position = adapter.getWatchMap().getPositionByKey(key);
                            adapter.notifyItemChanged(position);
                        }
                        NetWorkBuilder.doChunkRequestAsync(NetWorkBuilder.buildJsonRequestJwt(
                                ServerURL.URL_WATCH_DELETE, ServerURL.POST, new WatchDeleteReq(watch.clientId)
                        )).thenAcceptAsync(response -> {
                            //回调的时候必须重新尝试获取引用，避免用户多次操作导致这个引用已经删除了
                            Watch watch1 = adapter.getWatchMap().getByKey(key);
                            if (watch1 == null) {
                                return;
                            }
                            NetWorkHandler.handleNetRespAfterLogin(
                                    null,
                                    response,
                                    msg -> {//还原名字
                                        watch1.name = tempName;
                                        doNotification(msg);
                                    },
                                    ignore -> {
                                        int position = adapter.getWatchMap().getPositionByKey(key);
                                        if (position >= 0) {//主线程统一更新，绝对安全
                                            adapter.getWatchMap().remove(key);
                                            adapter.notifyItemRemoved(position);
                                        }
                                    },
                                    Void.class
                            );
                        }, ContextCompat.getMainExecutor(GlobalApplication.getAppContext()));
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
        viewModel.goOnline.observe(getViewLifecycleOwner(), key -> {
            if (key == null) {
                return;
            }
            viewModel.goOnline.setValue(null);
            //首先检查一遍是否有已经上线的手表，如果有就提醒用户自己下线
            MyAdapter adapter = (MyAdapter) recyclerView.getAdapter();
            if (adapter == null) {
                return;
            }
            int position = adapter.getWatchMap().getPositionByKey(key);
            long now = System.currentTimeMillis();
            for (int i = 0; i < adapter.getWatchMap().getSize(); ++i) {
                Pair<Integer, Watch> pair = adapter.getWatchMap().getByPosition(i);
                if (pair == null) {
                    return;
                }
                if (pair.getB().expireTime > now && pair.getB().activate > 0) {
                    doNotification("您有已经在拉取数据手表授权，请先下线");
                    adapter.notifyItemChanged(position);
                    return;
                }
            }
            Watch watch = adapter.getWatchMap().getByKey(key);
            if (watch == null) {
                return;
            }
            //需要根据authorizeHeader解析回secretKey
            String secretKey = EncryptThreadSafe.transferStringFromBase64EncodeedString(watch.authorizeHeader).split(":")[1];
            //弹出新的界面让用户授权
            requireParentFragment().getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.reportFragment, ReportAddDeviceFragment.getInstance(
                            watch.name,
                            watch.watchType,
                            watch.clientId,
                            secretKey
                    ))
                    .addToBackStack(null)
                    .commit();
        });
        viewModel.goOffline.observe(getViewLifecycleOwner(), key -> {
            if (key == null) {
                return;
            }
            viewModel.goOffline.setValue(null);
            new AlertDialog.Builder(requireContext())
                    .setTitle("")
                    .setMessage("您确定要让此设备下线吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        //首先更新UI为下线中
                        MyAdapter adapter = (MyAdapter) recyclerView.getAdapter();
                        if (adapter == null) {
                            return;
                        }
                        Watch watch = adapter.getWatchMap().getByKey(key);
                        if (watch == null) {
                            return;
                        }
                        String tempName = watch.name;
                        {
                            int position = adapter.getWatchMap().getPositionByKey(key);
                            watch.name = watch.name + "（下线中）";
                            adapter.notifyItemChanged(position);
                        }
                        //网络请求进行下线
                        NetWorkBuilder.doChunkRequestAsync(NetWorkBuilder.buildJsonRequestJwt(
                                ServerURL.URL_GO_OFFLINE, ServerURL.POST, new WatchDeleteReq(watch.clientId)
                        )).thenAcceptAsync(response -> {
                            Watch watch1 = adapter.getWatchMap().getByKey(key);
                            if (watch1 == null) {
                                return;
                            }
                            //不论成功还是失败都要变回原来的名字
                            int position = adapter.getWatchMap().getPositionByKey(key);
                            watch1.name = tempName;
                            NetWorkHandler.handleNetRespAfterLogin(
                                    null,
                                    response,
                                    this::doNotification,
                                    ignore -> {
                                        if (position >= 0) {
                                            watch.activate = 0;
                                        }
                                    },
                                    Void.class
                            );
                            adapter.notifyItemChanged(position);
                        }, ContextCompat.getMainExecutor(GlobalApplication.getAppContext()));
                    })
                    .setNegativeButton("取消", ((dialog, which) -> {
                        MyAdapter adapter = (MyAdapter) recyclerView.getAdapter();
                        if (adapter == null) {
                            return;
                        }
                        adapter.notifyItemChanged(adapter.getWatchMap().getPositionByKey(key));

                    }))
                    .show();
        });
        //进行网络请求，然后更新数据
        currentRunning.setText("网络请求数据中...");
        NetWorkBuilder.doChunkRequestAsync(NetWorkBuilder.buildJsonRequestJwt(
                ServerURL.URL_GET_WATCH_LIST,
                ServerURL.POST, null
        )).thenAcceptAsync(response -> NetWorkHandler.handleNetRespAfterLogin(
                null,
                response,
                msg -> {
                    doNotification(msg);
                    currentRunning.setText("网络请求失败");
                },
                content -> {
                    if (content.watchList == null || content.watchList.isEmpty()) {
                        doNotification("您还没有添加手表哦");
                        currentRunning.setText("这里空空如也~");
                        return;
                    }
                    viewModel.watchList.setValue(content.watchList);
                    currentRunning.setText("");
                },
                WatchGetResp.class
        ), ContextCompat.getMainExecutor(GlobalApplication.getAppContext()));
    }

    private void onNotificationClicked(View v) {
        notification.setImageDrawable(ContextCompat.getDrawable(
                GlobalApplication.getAppContext(), R.drawable.notifications));
        drawerLayout.openDrawer(GravityCompat.END);
    }

    private void doNotification(String msg) {
        //使用balloon来提醒用户
        Context context = requireContext();
        Balloon balloon = new Balloon.Builder(context)
                .setArrowSize(10)
                .setArrowOrientation(ArrowOrientation.TOP)
                .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                .setArrowPosition(0.5f)
                .setWidth(BalloonSizeSpec.WRAP)
                .setHeight(65)
                .setPaddingHorizontal(12)
                .setTextSize(15f)
                .setCornerRadius(4f)
                .setAlpha(0.9f)
                .setText(msg)
                .setTextColor(ContextCompat.getColor(context, R.color.black))
                .setTextIsHtml(false)
                .setBackgroundColor(ContextCompat.getColor(context, R.color.recordRecordDetail))
                .setBalloonAnimation(BalloonAnimation.FADE)
                .setLifecycleOwner(getViewLifecycleOwner())
                .build();
        balloon.showAlignBottom(this.notification);
        balloon.dismissWithDelay(2000L);//2秒自己结束
        //添加消息到列表中，用户可以滑动查看消息
        notificationList.add(msg);
        ReportAddDeviceFragment.MyAdapter myAdapter = (ReportAddDeviceFragment.MyAdapter) notificationRecycler.getAdapter();
        if (myAdapter == null) {
            return;
        }
        myAdapter.notifyItemInserted(notificationList.size() - 1);
        //如果成功插入，更新图标提醒用户
        this.notification.setImageDrawable(ContextCompat.getDrawable(
                GlobalApplication.getAppContext(), R.drawable.notification_unread
        ));
    }

    public void replace(Fragment fragment) {
        if (fragment == null) {
            return;
        }
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.reportFragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView.setAdapter(null);
    }

    public static class MyViewModel extends ViewModel {
        public MutableLiveData<List<Watch>> watchList = new MutableLiveData<>();
        public MutableLiveData<Integer> goOnline = new MutableLiveData<>();
        public MutableLiveData<Integer> goOffline = new MutableLiveData<>();
        public MutableLiveData<Integer> deleteProxy = new MutableLiveData<>();
    }

    private static class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
        private final LinearHashMap<Integer, Watch> watchMap = new LinearHashMap<>(4);
        final private MyViewModel viewModel;
        private final AtomicInteger PRIMARY_KEY = new AtomicInteger(0);

        public MyAdapter(MyViewModel viewModel) {
            this.viewModel = viewModel;
        }

        public void setWatchList(List<Watch> watchList) {
            this.watchMap.clear();
            if (watchList == null) {
                return;
            }
            for (Watch watch : watchList) {
                this.watchMap.put(PRIMARY_KEY.getAndIncrement(), watch);
            }
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MyViewHolder(
                    viewModel,
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_report_device, parent, false)
            );
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            Pair<Integer, Watch> watchPair = watchMap.getByPosition(position);
            if (watchPair == null) {
                return;
            }
            Watch watch = watchPair.getB();
            long now = System.currentTimeMillis();
            holder.setAll(
                    watch.name,
                    watch.watchType,
                    watch.expireTime > now && watch.activate > 0,
                    watchPair.getA()
            );
        }

        public LinearHashMap<Integer, Watch> getWatchMap() {
            return watchMap;
        }

        @Override
        public int getItemCount() {
            return watchMap.getSize();
        }
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final MyViewModel viewModel;
        public TextView watchName;
        public TextView watchTypeAndStatus;
        public ImageView bigImage;
        public SwitchCompat goOnline;
        public TextView lineText;
        public ImageView deleteDevice;
        public int watchKey = -1;

        public MyViewHolder(
                MyViewModel viewModel,
                @NonNull View itemView) {
            super(itemView);
            this.viewModel = viewModel;
            this.watchName = itemView.findViewById(R.id.watchName);
            this.watchTypeAndStatus = itemView.findViewById(R.id.watchType);
            this.bigImage = itemView.findViewById(R.id.bigImage);
            this.goOnline = itemView.findViewById(R.id.goOnline);
            this.lineText = itemView.findViewById(R.id.lineText);
            this.deleteDevice = itemView.findViewById(R.id.deleteDevice);
            this.deleteDevice.setOnClickListener(this::onDeleteDeviceClicked);
        }

        public void setAll(String name, String type, boolean onLine, int watchKey) {
            watchName.setText(name);
            this.watchKey = watchKey;
            watchTypeAndStatus.setText(String.format("%s（%s）",
                    type,
                    onLine ? "在线" : "离线"));
            goOnline.setOnCheckedChangeListener(null);
            if (onLine) {
                goOnline.setChecked(true);
                lineText.setText("下线");
            } else {
                goOnline.setChecked(false);
                lineText.setText("上线");
            }
            goOnline.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {//进行上线
                    viewModel.goOnline.setValue(watchKey);
                } else {//进行下线
                    viewModel.goOffline.setValue(watchKey);
                }
            });
        }

        private void onDeleteDeviceClicked(View v) {
            //让viewModel进行代理执行逻辑
            //这个是有原因的，因为这里面涉及到网络请求，如果贸然在这里执行UI更新请求
            //我们是不知道用户此时的页面状态的，是有可能这个页面已经被销毁了，这个时候网络回调就会很危险
            //这里必须new出一个新对象，因为ViewHodler会复用，导致提交的信息有问题
            viewModel.deleteProxy.setValue(watchKey);
        }
    }
}

class LinearHashMap<K, V> {
    private int size = 0;
    private int cap;
    Object[] list;
    int[] positionList;

    public LinearHashMap(int initCap) {
        this.cap = initCap;
        list = new Object[initCap];
        positionList = new int[initCap];
        Arrays.fill(positionList, -1);
    }

    public int getSize() {
        return size;
    }


    @SuppressWarnings({"unchecked"})
    synchronized public void put(K k, V v) {
        if (size == cap) {//需要扩容
            int newCap = cap << 1;
            Object[] tempList = new Object[newCap];
            int[] newPositionList = new int[newCap];
            Arrays.fill(newPositionList, -1);
            int count = 0;
            for (int i = 0; i < cap; ++i) {
                int oldIndex = positionList[i];
                Object o = list[oldIndex];
                Pair<K, V> pair = (Pair<K, V>) o;
                int kHashCode = pair.getA().hashCode();
                int newIndex = kHashCode & (newCap - 1);
                for (int j = 0; j < newCap; ++j) {
                    int tempIndex = j + newIndex;
                    if (tempIndex >= newCap) {
                        tempIndex &= (newCap - 1);
                    }
                    if (tempList[tempIndex] == null) {
                        tempList[tempIndex] = o;
                        newPositionList[count] = tempIndex;
                        ++count;
                        break;
                    }
                }
            }
            positionList = newPositionList;
            list = tempList;
            cap = newCap;
        }
        int hashCode = k.hashCode();
        int index = hashCode & (cap - 1);
        int insertIndex = 0;
        for (int i = 0; i < cap; ++i) {
            int tempIndex = i + index;
            if (tempIndex >= cap) {
                tempIndex &= (cap - 1);
            }
            if (list[tempIndex] == null) {
                list[tempIndex] = new Pair<>(k, v);
                insertIndex = tempIndex;
                break;
            }
            Pair<K, V> pair = (Pair<K, V>) list[tempIndex];
            if (pair.getA().equals(k)) {
                pair.setB(v);
                return;
            }
        }
        //更新position，这个元素的position就是size
        positionList[size] = insertIndex;
        ++size;
    }

    @SuppressWarnings({"unchecked"})
    synchronized public void remove(K k) {
        int index = k.hashCode() & (cap - 1);
        for (int i = 0; i < cap; ++i) {
            int tempIndex = i + index;
            if (tempIndex >= cap) {
                tempIndex &= (cap - 1);
            }
            Pair<K, V> pair = (Pair<K, V>) list[tempIndex];
            if (pair == null) {
                continue;
            }
            if (pair.getA().equals(k)) {
                list[tempIndex] = null;
                --size;
                //所有元素往前移动
                for (int j = 0; j < cap; ++j) {
                    if (positionList[j] == tempIndex) {
                        int replaceIndex = j;
                        for (int l = j + 1; l < cap; ++l) {
                            positionList[replaceIndex] = positionList[l];
                            replaceIndex = l;
                            if (positionList[l] < 0) {
                                break;
                            }
                            if (l == cap - 1) {
                                positionList[l] = -1;
                            }
                        }
                        break;
                    }
                }
                return;
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    @Nullable
    synchronized public V getByKey(K k) {
        int index = k.hashCode() & (cap - 1);
        for (int i = 0; i < cap; ++i) {
            int tempIndex = i + index;
            if (tempIndex >= cap) {
                tempIndex &= (cap - 1);
            }
            Pair<K, V> pair = (Pair<K, V>) list[tempIndex];
            if (pair == null) {//从未添加过
                continue;
            }
            if (pair.getA().equals(k)) {
                return pair.getB();
            }
        }
        return null;
    }

    /**
     * 必须处理position为-1的情况，避免NPE
     */
    @SuppressWarnings({"unchecked"})
    synchronized public int getPositionByKey(K k) {
        int index = k.hashCode() & (cap - 1);
        for (int i = 0; i < cap; ++i) {
            int tempIndex = i + index;
            if (tempIndex >= cap) {
                tempIndex &= (cap - 1);
            }
            Pair<K, V> pair = (Pair<K, V>) list[tempIndex];
            if (pair == null) {
                continue;
            }
            if (pair.getA().equals(k)) {
                for (int j = 0; j < cap; ++j) {
                    if (positionList[j] == tempIndex) {
                        return j;
                    }
                }
                return -1;
            }
        }
        return -1;
    }

    @SuppressWarnings({"unchecked"})
    @Nullable
    synchronized public Pair<K, V> getByPosition(int position) {
        if (position < 0 || position >= cap) {
            return null;
        }
        int index = positionList[position];
        if (index == -1) {
            return null;
        }
        return ((Pair<K, V>) list[index]);
    }

    synchronized public void clear() {
        this.size = 0;
        Arrays.fill(list, null);
        Arrays.fill(positionList, -1);
    }
}