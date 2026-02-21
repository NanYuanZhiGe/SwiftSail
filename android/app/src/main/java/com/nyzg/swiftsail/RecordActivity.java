package com.nyzg.swiftsail;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.nyzg.swiftsail.bean.RecordType;
import com.nyzg.swiftsail.fragment.record.RecordJumpFragment;
import com.nyzg.swiftsail.fragment.record.RecordRecordFragment;
import com.nyzg.swiftsail.repository.RecordRecordRepository;

public class RecordActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_record);
        RecordRecordRepository.getInstance().SELECT_TYPE.observe(this, this::changeFragment);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void changeFragment(String type) {
        if (type != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            //走路或骑行模式
            if (type.equals(RecordType.WALK_OR_RUN) || type.equals(RecordType.BIKE)) {
                transaction.replace(R.id.baseFragment, RecordRecordFragment.getInstance());
            } else {
                switch (type) {
                    case RecordType.JUMP_ROPE:
                        transaction.replace(R.id.baseFragment, RecordJumpFragment.getInstance());
                        break;
                    case RecordType.YOGA:
                        break;
                    case RecordType.BALL:
                }
            }
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }
}
