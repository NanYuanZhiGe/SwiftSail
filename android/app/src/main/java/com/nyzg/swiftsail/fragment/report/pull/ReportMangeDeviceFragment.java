package com.nyzg.swiftsail.fragment.report.pull;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.dao.WatchTable;
import com.nyzg.swiftsail.dbobj.Watch;
import com.nyzg.swiftsail.fragment.InnerFragment;
import com.nyzg.swiftsail.repository.WatchRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReportMangeDeviceFragment extends InnerFragment implements ReplaceListener {
    public static Fragment getInstance() {
        return new ReportMangeDeviceFragment();
    }

    private RecyclerView recyclerView;

    @SuppressLint("NotifyDataSetChanged")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_report_device_mange, container, false);
        recyclerView = father.findViewById(R.id.devices);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(new MyAdapter(this));
        WatchRepository.getInstance().watchList.observe(getViewLifecycleOwner(), watches -> {
            MyAdapter myAdapter = (MyAdapter) recyclerView.getAdapter();
            if (myAdapter == null) {
                return;
            }
            if (watches == null) {
                myAdapter.setWatchList(new ArrayList<>());
            } else {
                myAdapter.setWatchList(watches);
            }
            myAdapter.notifyDataSetChanged();
        });
        return father;
    }

    @Override
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

    private static class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {

        private List<Watch> watchList = new ArrayList<>();
        private final ReplaceListener replaceListener;

        public MyAdapter(ReplaceListener replaceListener) {
            this.replaceListener = replaceListener;
        }

        public void setWatchList(List<Watch> watchList) {
            this.watchList = new ArrayList<>(watchList.size());
            this.watchList.addAll(watchList);
            this.watchList.add(new Watch());
            this.watchList.add(new Watch());
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MyViewHolder(
                    replaceListener,
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_report_device, parent, false)
            );
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            Watch watch = watchList.get(position);
            if (watch.name != null && !watch.name.isEmpty()) {
                holder.firstCharacter.setText(watch.name.charAt(0));
                holder.name.setText(watch.name);
            }
            if (watch.accessible) {
                holder.status.setText("在线");
                holder.status.setTextColor(ContextCompat.getColor(GlobalApplication.getAppContext(), R.color.lightGreen));
            } else {
                holder.status.setText("授权过期");
                holder.status.setTextColor(ContextCompat.getColor(GlobalApplication.getAppContext(), R.color.lightRed));
            }
            holder.id = watch.id;
        }

        @Override
        public int getItemCount() {
            Log.v("myTag",watchList.size()+"");
            return watchList.size();
        }
    }

    private static class MyViewHolder extends RecyclerView.ViewHolder {
        final private TextView firstCharacter;
        final private TextView name;
        final private TextView status;
        private long id = -1L;
        private final ReplaceListener replaceListener;

        public MyViewHolder(
                ReplaceListener replaceListener,
                @NonNull View itemView) {
            super(itemView);
            this.replaceListener = replaceListener;
            firstCharacter = itemView.findViewById(R.id.firstCharacter);
            name = itemView.findViewById(R.id.name);
            status = itemView.findViewById(R.id.status);
            View reset = itemView.findViewById(R.id.resetDevice);
            reset.setOnClickListener(this::onResetClicked);
            View delete = itemView.findViewById(R.id.deleteDevice);
            delete.setOnClickListener(this::onDeleteClicked);
        }

        private void onResetClicked(View v) {
            replaceListener.replace(ReportAddDeviceFragment.getInstance());
        }

        private void onDeleteClicked(View v) {
            //本地删除这个设备，不再显示
            //下一次添加服务端会自动处理冲突
            CompletableFuture.supplyAsync(() -> {
                WatchTable watchTable = SQLiteDB.getDatabase(GlobalApplication.getAppContext()).watchTable();
                watchTable.deleteWatch(id);
                WatchRepository.getInstance().deleteWatchThreadSafe(id);
                return null;
            });
        }
    }
}

interface ReplaceListener {
    void replace(Fragment fragment);
}