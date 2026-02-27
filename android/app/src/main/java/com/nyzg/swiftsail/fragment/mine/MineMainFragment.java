package com.nyzg.swiftsail.fragment.mine;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.MainActivity;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.bean.ImageUtils;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.bean.ServerURL;
import com.nyzg.swiftsail.dao.LastLoginTable;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.fragment.login.LoginFragment;
import com.nyzg.swiftsail.repository.LoginRepository;
import com.nyzg.swiftsail.repository.NetDataRepository;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MineMainFragment extends Fragment {
    public static Fragment getInstance() {
        return new MineMainFragment();
    }

    @SuppressLint("DefaultLocale")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_mine_base, container, false);
        //设置点击事件
        father.findViewById(R.id.accountName).setOnClickListener(this::onUserEntranceClicked);
        father.findViewById(R.id.funcSecurity).setOnClickListener(this::onSecurityClicked);
        father.findViewById(R.id.funcMailBox).setOnClickListener(this::onMailBoxClicked);
        father.findViewById(R.id.funcCollection).setOnClickListener(this::onCollectionClicked);
        father.findViewById(R.id.funcSync).setOnClickListener(this::onSyncClicked);
        father.findViewById(R.id.funcBottle).setOnClickListener(this::onBottleClicked);
        father.findViewById(R.id.funcLogout).setOnClickListener(this::onLogoutClicked);
        //监听登录用户的变化，更新UI
        LoginRepository.getInstance().getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user == null) {
                return;
            }
            ((TextView) father.findViewById(R.id.accountName)).setText(user.nickName);
            ((TextView) father.findViewById(R.id.accountId)).setText(String.format("账户id：%d", user.id));
            //--头像--
            Glide.with(requireContext())
                    .load(ImageUtils.getHeadIconURI(user.id))
                    .error(R.drawable.image)
                    .into((ImageView) father.findViewById(R.id.headImage));
        });
        return father;
    }

    private void onUserEntranceClicked(View view) {
        requireParentFragment().getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.mineFragment, UserDataFragment.getInstance())
                .addToBackStack(null)
                .commit();
    }

    private void onSecurityClicked(View view) {
        requireParentFragment().getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.mineFragment, MineSecurityFragment.getInstance())
                .addToBackStack(null)
                .commit();
    }

    private void onMailBoxClicked(View view) {

    }

    private void onCollectionClicked(View view) {

    }

    private void onSyncClicked(View view) {

    }

    private void onBottleClicked(View view) {

    }

    private void onLogoutClicked(View view) {
        new AlertDialog.Builder(requireContext())
                .setTitle("")
                .setMessage("您确定要退出登录吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    view.setOnClickListener(v -> {
                    });
                    CompletableFuture.supplyAsync(() -> {
                        //首先清除上一次登录
                        //不然用户退出App下一次登录又会自动登录
                        SQLiteDB sqLiteDB = SQLiteDB.getDatabase(GlobalApplication.getAppContext());
                        LastLoginTable lastLoginTable = sqLiteDB.lastLoginTable();
                        lastLoginTable.deleteAll();
                        //然后查询可用用户列表，用于登录选项
                        List<User> availableUserList = sqLiteDB.userTable().selectAllUser();
                        //需要额外添加一个用户：“账号未列出？”
                        User unListUser = new User();
                        unListUser.id = -1L;
                        availableUserList.add(unListUser);
                        LoginRepository.getInstance().availableUserList.postValue(availableUserList);
                        return null;
                    }).thenAcceptAsync(action -> {
                        view.setOnClickListener(this::onLogoutClicked);
                        LoginRepository.getInstance().setCurrentUser(null);
                        MainActivity.addFragmentToStackTop(
                                requireActivity().getSupportFragmentManager(),
                                LoginFragment.newInstance()
                        );
                    }, ContextCompat.getMainExecutor(requireContext()));
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
