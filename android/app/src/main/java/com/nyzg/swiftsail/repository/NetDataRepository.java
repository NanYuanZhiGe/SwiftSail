package com.nyzg.swiftsail.repository;

import static androidx.core.content.ContentProviderCompat.requireContext;

import androidx.lifecycle.MutableLiveData;

import com.bumptech.glide.Glide;
import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.bean.ServerURL;
import com.nyzg.swiftsail.dbobj.User;

import java.net.URI;

public class NetDataRepository {
    private static volatile NetDataRepository self;

    public MutableLiveData<Integer> headIconNotifier = new MutableLiveData<>(0);

    private NetDataRepository() {
        NetDataRepository.getInstance().headIconNotifier.observeForever(i -> {
            User user = LoginRepository.getInstance().currentUser.getValue();
            if (user == null || user.id == GlobalInstance.LOCAL_USER.id) {
                return;
            }
            //--刷新头像--
            try {
                Glide.with(GlobalApplication.getAppContext())
                        .load(new URI(ServerURL.BASE_IMAGE_URL + user.id + ServerURL.HEAD_ICON_SUFFIX))
                        .skipMemoryCache(true)
                        .error(R.drawable.image)
                        .submit();
            } catch (Exception ignore) {
            }
        });
    }

    public static NetDataRepository getInstance() {
        if (self != null) {
            return self;
        }
        synchronized (NetDataRepository.class) {
            if (self != null) {
                return self;
            }
            self = new NetDataRepository();
            return self;
        }
    }
}
