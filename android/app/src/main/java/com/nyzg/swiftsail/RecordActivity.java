package com.nyzg.swiftsail;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.nyzg.swiftsail.fragment.record.RecordRecordFragment;
import com.nyzg.swiftsail.fragment.record.RecordSelectFragment;
import com.nyzg.swiftsail.view.SportLayout;
import com.nyzg.swiftsail.viewmodel.RecordSelectViewModel;

public class RecordActivity extends AppCompatActivity {
    private RecordSelectViewModel recordSelectViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_record);

        this.recordSelectViewModel = new ViewModelProvider(this).get(RecordSelectViewModel.class);
        recordSelectViewModel.getSelectType().observe(this, this::changeFragment);

        ((SportLayout) findViewById(R.id.walkOrRun)).setRecordSelectViewModel(recordSelectViewModel);
        ((SportLayout) findViewById(R.id.bike)).setRecordSelectViewModel(recordSelectViewModel);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void changeFragment(String type) {
        if (type != null && RecordSelectFragment.TYPE_SET.contains(type)) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.baseFragment, RecordRecordFragment.getInstance(type));
            transaction.addToBackStack(null);
            transaction.commit();
            //切换完fragment后一定要重新置空viewModel，不然用户的下一次点击同一个选项是没有效果的
            recordSelectViewModel.setNull();
        }
    }
}
