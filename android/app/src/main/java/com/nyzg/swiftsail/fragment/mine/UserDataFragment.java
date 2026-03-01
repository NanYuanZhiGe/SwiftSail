package com.nyzg.swiftsail.fragment.mine;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.GlobalInstance;
import com.nyzg.swiftsail.bean.GlobalToast;
import com.nyzg.swiftsail.bean.NetWorkBuilder;
import com.nyzg.swiftsail.bean.NetWorkHandler;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.bean.ServerURL;
import com.nyzg.swiftsail.netobj.PublicImageReq;
import com.nyzg.swiftsail.netobj.PublicImageResp;
import com.nyzg.swiftsail.repository.LoginRepository;
import com.nyzg.swiftsail.repository.NetDataRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public class UserDataFragment extends Fragment {

    public static Fragment getInstance() {
        return new UserDataFragment();
    }

    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日");


    //从用户的相册选择图片
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    @SuppressLint("DefaultLocale")
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //注册图片选择器
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null
                            || LoginRepository.getInstance().currentUser.getValue() == null
                            || LoginRepository.getInstance().currentUser.getValue().id == GlobalInstance.LOCAL_USER.id) {
                        return;
                    }
                    final long userId = LoginRepository.getInstance().currentUser.getValue().id;
                    final String tempFileName = String.format("%d-headicon-temp.jpeg", userId);
                    CompletableFuture.runAsync(() -> {
                        try {
                            //压缩图片
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
                            int width = bitmap.getWidth();
                            int height = bitmap.getHeight();
                            int size = Math.min(width, height);
                            int cropX = (width - size) >> 1;
                            int cropY = (height - size) >> 1;
                            Bitmap centerCropBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, size, size);
                            File tempFile = new File(requireContext().getCacheDir(), tempFileName);
                            int quality = 90;
                            if (tempFile.length() > 10 * 1024 * 1024) {
                                quality = 40;
                            } else if (tempFile.length() > 5 * 1024 * 1024) {
                                quality = 70;
                            } else if (tempFile.length() > 1024 * 1024) {
                                quality = 80;
                            }
                            while (quality >= 10) {
                                if (tempFile.exists()) {
                                    tempFile.delete();
                                }
                                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                                    centerCropBitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);
                                }
                                if (tempFile.length() <= 128 * 1024) {
                                    break;
                                }
                                if (tempFile.length() > 10 * 1024 * 1024) {
                                    quality = 10;
                                } else if (quality >= 20) {
                                    quality -= 10;
                                }
                            }
                            bitmap.recycle();
                            centerCropBitmap.recycle();
                            //上传至云
                            byte[] data = new byte[(int) tempFile.length()];
                            try (FileInputStream fis = new FileInputStream(tempFile)) {
                                fis.read(data, 0, (int) tempFile.length());
                            }
                            NetWorkHandler.handleNetRespAfterLogin(
                                    null,
                                    NetWorkBuilder.doChunkRequest(NetWorkBuilder.buildJsonRequestJwt(
                                            ServerURL.URL_SEND_SMALL_IMAGE_HEAD, ServerURL.POST, new PublicImageReq(data)
                                    )),
                                    msg -> GlobalToast.COMMON_TOAST.accept("图片上传失败：" + msg),
                                    resp -> {
                                        if (resp == null || resp.fileName == null) {
                                            return;
                                        }
                                        String name = ServerURL.BASE_IMAGE_URL + resp.fileName;
                                        NetDataRepository.getInstance().headIconNotifier.postValue(name);
                                        SQLiteDB.getDatabase(GlobalApplication.getAppContext())
                                                .shareTable()
                                                .insertIfNotExist(userId, "headIconUrl", name);
                                    },
                                    PublicImageResp.class
                            );
                        } catch (Exception e) {
                            GlobalToast.COMMON_TOAST.accept("图片上传失败");
                        }
                    });
                }
        );
    }

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_mine_user_data, container, false);
        ImageView headIcon = father.findViewById(R.id.headIcon);
        NetDataRepository.getInstance().headIconNotifier.observe(getViewLifecycleOwner(),
                url -> Glide.with(requireContext())
                        .load(url)
                        .error(R.drawable.image)
                        .into(headIcon));
        headIcon.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        LoginRepository.getInstance().getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            ((TextView) father.findViewById(R.id.nickName).findViewById(R.id.content)).setText(user.nickName);
            ((TextView) father.findViewById(R.id.id).findViewById(R.id.content)).setText(user.id + "");
            ((TextView) father.findViewById(R.id.email).findViewById(R.id.content)).setText(user.email);
            ((TextView) father.findViewById(R.id.createTime).findViewById(R.id.content)).setText(LocalDate.ofEpochDay(user.createTime).format(DATE_TIME_FORMATTER));
        });
        return father;
    }
}