package com.nyzg.swiftsail.fragment.geo;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.textfield.TextInputEditText;
import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.NetWorkBuilder;
import com.nyzg.swiftsail.bean.NetWorkHandler;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.bean.ServerURL;
import com.nyzg.swiftsail.dbobj.PersonalData;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.netobj.geo.PersonalInfo;
import com.nyzg.swiftsail.obj.SucceedOrNot;
import com.nyzg.swiftsail.repository.GeoRepository;
import com.nyzg.swiftsail.repository.LoginRepository;
import com.nyzg.swiftsail.view.BackWardLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class GeoEditUserData extends Fragment {

    public static Fragment getInstance() {
        return new GeoEditUserData();
    }


    private TextInputEditText appellationText;
    private TextInputEditText descriptionText;
    private ImageView backgroundPreview;
    private TextView imageCount;
    private RecyclerView layoutsRecycler;
    private Spinner genderText;
    private String backgroundUri;

    private MyViewModel myViewModel;

    final private List<Uri> layoutsList = new ArrayList<>(9);
    private ActivityResultLauncher<String> backgroundPickLauncher;
    private ActivityResultLauncher<String> layoutsPickLauncher;
    private OnBackPressedCallback callback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myViewModel = new ViewModelProvider(this).get(MyViewModel.class);
        if (myViewModel.layouts != null) {
            this.layoutsList.clear();
            this.layoutsList.addAll(myViewModel.layouts);
        }
        callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBackPressed();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(callback);
        layoutsPickLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uriList -> {
                    if (imageCount == null
                            || layoutsRecycler == null
                            || layoutsRecycler.getAdapter() == null
                            || layoutsList.size() == 9
                            || uriList == null
                            || uriList.isEmpty()) {
                        return;
                    }
                    MyAdapter myAdapter = (MyAdapter) layoutsRecycler.getAdapter();
                    int size = layoutsList.size();
                    int count = 0;
                    for (int i = 0; i < uriList.size() && layoutsList.size() < 9; ++i) {
                        layoutsList.add(uriList.get(i));
                        ++count;
                    }
                    myAdapter.notifyItemRangeInserted(size, count);
                    myViewModel.layouts = layoutsList;
                    updateLayoutCount();
                });
        backgroundPickLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (backgroundPreview == null) {
                        return;
                    }
                    try {
                        backgroundPreview.setImageBitmap(MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri));
                        backgroundUri = uri.toString();
                        myViewModel.background = uri;
                    } catch (Exception e) {
                        backgroundPreview.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.image));
                        backgroundUri = null;
                        myViewModel.background = null;
                    }
                });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.callback.remove();
    }

    private void onBackPressed() {
        if (this.containsUnSafeData()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("")
                    .setMessage("您退出后数据不会得到保存！确定要离开吗？")
                    .setPositiveButton("确定", (dig, i) -> {
                        callback.setEnabled(false);
                        requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    })
                    .setNegativeButton("取消", null)
                    .create()
                    .show();
        } else {
            callback.setEnabled(false);
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        }
    }

    private boolean containsUnSafeData() {
        Editable appellation = appellationText.getText();
        Editable description = descriptionText.getText();
        return (appellation != null && appellation.length() > 0)
                || (description != null && description.length() > 0)
                || !layoutsList.isEmpty()
                || backgroundUri != null;
    }

    @SuppressLint("DefaultLocale")
    private void updateLayoutCount() {
        imageCount.setText(String.format("%d/9", layoutsList.size()));
    }

    private BackWardLayout backWardLayout;

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_geo_user_data, container, false);
        backWardLayout = father.findViewById(R.id.backwardLayout);
        backWardLayout.findViewById(R.id.backward).setOnClickListener(v -> onBackPressed());
        descriptionText = father.findViewById(R.id.descriptionText);
        //解决滑动冲突的问题
        NestedScrollView nestedScrollView = father.findViewById(R.id.nestedScrollView);
        descriptionText.setOnTouchListener((v, event) -> {
            if (descriptionText.canScrollVertically(1) || descriptionText.canScrollVertically(-1)) {
                nestedScrollView.requestDisallowInterceptTouchEvent(true);
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    nestedScrollView.requestDisallowInterceptTouchEvent(false);
                }
            }
            return false;
        });
        TextView textCount = father.findViewById(R.id.descriptionHint);
        if (myViewModel.description != null) {
            descriptionText.setText(myViewModel.description);
        }
        descriptionText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @SuppressLint({"DefaultLocale", "SetTextI18n"})
            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && s.length() > 0) {
                    textCount.setText(String.format("%d/1000", s.length()));
                    myViewModel.description = s.toString();
                } else {
                    textCount.setText("0/1000");
                    myViewModel.description = null;
                }
            }
        });
        //data
        genderText = father.findViewById(R.id.genderText);
        genderText.setAdapter(ArrayAdapter.createFromResource(
                requireContext(),
                R.array.gender,
                R.layout.spinner_layout
        ));
        genderText.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                myViewModel.gender = (short) position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        genderText.setSelection(myViewModel.gender);
        appellationText = father.findViewById(R.id.appellationText);
        appellationText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && s.length() > 0) {
                    myViewModel.appellation = s.toString();
                } else {
                    myViewModel.appellation = null;
                }
            }
        });
        if (myViewModel.appellation != null) {
            appellationText.setText(myViewModel.appellation);
        }

        //选择背景图片
        backgroundPreview = father.findViewById(R.id.backgroundPreview);
        try {
            if (myViewModel.background != null) {
                backgroundPreview.setImageBitmap(MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), myViewModel.background));
            }
        } catch (Exception ignore) {
            backgroundPreview.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.image));
        }
        father.findViewById(R.id.backgroundButton).setOnClickListener(v -> backgroundPickLauncher.launch("image/*"));
        father.findViewById(R.id.layouts).setOnClickListener(v -> layoutsPickLauncher.launch("image/*"));
        imageCount = father.findViewById(R.id.imageCount);
        //必须再recyclerView之前执行
        PersonalData personalData = GeoRepository.getInstance().personalData.getValue();
        if (personalData != null) {
            appellationText.setText(personalData.appellation);
            genderText.setSelection(personalData.gender);
            descriptionText.setText(personalData.description);
        }
        layoutsRecycler = father.findViewById(R.id.layoutsRecycler);
        layoutsRecycler.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        layoutsRecycler.setAdapter(new MyAdapter());
        //提交
        father.findViewById(R.id.confirmButton).setOnClickListener(this::onConfirmBtnClicked);
        return father;
    }

    private void onConfirmBtnClicked(View v) {
        //收集数据
        if (!containsUnSafeData()) {//没有数据提交
            return;
        }
        this.backWardLayout.setTitlePrefix("编辑个人信息");
        this.backWardLayout.setTitleStatus("（提交中）");
        final PersonalInfo personalInfo = new PersonalInfo();
        personalInfo.appellation = myViewModel.appellation;
        personalInfo.description = myViewModel.description;
        personalInfo.gender = myViewModel.gender;
        final Uri bk = myViewModel.background;
        final List<Uri> layoutList = new ArrayList<>(myViewModel.layouts.size());
        layoutList.addAll(myViewModel.layouts);
        Lifecycle lifecycle = getLifecycle();
        User user = LoginRepository.getInstance().currentUser.getValue();
        final long userId = user != null ? user.id : 0L;
        CompletableFuture.supplyAsync(() -> {
            //压缩图像
            try {
                if (bk!=null){
                    personalInfo.background = shrinkImage(
                            MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), bk)
                    );
                }
                List<byte[]> list = new ArrayList<>(layoutList.size());
                for (int i = 0; i < layoutList.size(); ++i) {
                    byte[] image = shrinkImage(MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), layoutList.get(i)));
                    if (image!=null){
                        list.add(image);
                    }
                }
                personalInfo.layouts = list;
            } catch (Exception e) {
                personalInfo.background = null;
                personalInfo.layouts = null;
            }
            final AtomicReference<SucceedOrNot> result = new AtomicReference<>(SucceedOrNot.SUCCEED);
            NetWorkHandler.handleNetRespAfterLogin(
                    null,
                    NetWorkBuilder.doChunkRequest(NetWorkBuilder.buildJsonRequestJwt(
                            ServerURL.URL_UPDATE_PERSONAL_INFO, ServerURL.POST, personalInfo
                    )),
                    msg -> result.set(SucceedOrNot.FAIL),
                    resp -> {
                        //更新数据库中的内容
                        PersonalData personalData = new PersonalData(
                                personalInfo.appellation,
                                resp.bkImage,
                                resp.communicateScore,
                                resp.cooperationScore,
                                personalInfo.description,
                                personalInfo.gender,
                                resp.inTimeScore,
                                resp.layoutImage,
                                resp.levelScore,
                                resp.promiseScore,
                                userId
                        );
                        SQLiteDB.getDatabase(GlobalApplication.getAppContext())
                                .personalDataTable()
                                .insertPersonalData(personalData);
                        GeoRepository.getInstance().personalData.postValue(personalData);
                    },
                    PersonalData.class
            );
            return result.get();
        }).thenAcceptAsync(res -> {
            Lifecycle.State currentState = lifecycle.getCurrentState();
            if (currentState == Lifecycle.State.DESTROYED) {
                return;
            }
            if (res == SucceedOrNot.SUCCEED) {
                backWardLayout.setTitleStatus("（提交成功）");
            } else {
                backWardLayout.setTitleStatus("（提交失败）");
            }
        }, ContextCompat.getMainExecutor(GlobalApplication.getAppContext()));
    }

    @Nullable
    private byte[] shrinkImage(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = Math.min(width, height);
        int cropX = (width - size) >> 1;
        int cropY = (height - size) >> 1;
        Bitmap centerCropBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, size, size);
        File tempFile = new File(requireContext().getCacheDir(), "tmp-GeoEditUserData.jpeg");
        int quality = 90;
        if (tempFile.length() > 10 * 1572864L) {//1.5MB
            quality = 40;
        } else if (tempFile.length() > 5 * 1572864L) {
            quality = 70;
        } else if (tempFile.length() > 1572864L) {
            quality = 80;
        }
        while (quality >= 10) {
            if (tempFile.exists()) {
                tempFile.delete();
            }
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                centerCropBitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);
            } catch (Exception e) {
                bitmap.recycle();
                centerCropBitmap.recycle();
                return null;
            }
            if (tempFile.length() <= 1572864L) {
                break;
            }
            if (tempFile.length() > 5 * 1572864L) {
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
        } catch (Exception e) {
            return null;
        }
        return data;
    }

    private class MyAdapter extends RecyclerView.Adapter<MyHolder> {

        @NonNull
        @Override
        public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MyHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_geo_user_edit_image, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull MyHolder holder, int position) {
            try {
                Glide.with(holder.image.getContext())
                        .load(layoutsList.get(position))
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .placeholder(R.drawable.image)
                        .into(holder.image);
            } catch (Exception e) {
                holder.image.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.image));
            }
            holder.deleteButton.setOnClickListener(v -> {
                int currentPosition = holder.getBindingAdapterPosition();
                if (currentPosition == RecyclerView.NO_POSITION || currentPosition >= layoutsList.size()) {
                    return;
                }
                layoutsList.remove(currentPosition);
                this.notifyItemRemoved(currentPosition);
                updateLayoutCount();
            });
        }

        @Override
        public int getItemCount() {
            return layoutsList.size();
        }
    }

    private static class MyHolder extends RecyclerView.ViewHolder {
        public ImageView image;
        public View deleteButton;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    /*
    ((32+1000)*3+2+(1+9)*1.5*1024*1024)/1024/1024
    ==15MB
    加上json的开销，是可以控制在32MB以内
     */
    public static class MyViewModel extends ViewModel {
        String appellation;//32 char max
        short gender;//0-male 1-female
        String description;//1000 char max
        Uri background;//less than 1.5MB
        List<Uri> layouts;//each image in layouts less than 1.5MB
    }
}
