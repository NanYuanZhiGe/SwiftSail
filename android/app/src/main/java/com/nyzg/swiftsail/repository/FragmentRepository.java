package com.nyzg.swiftsail.repository;

import androidx.fragment.app.Fragment;

import java.util.HashMap;
import java.util.Map;

public class FragmentRepository {
    private static volatile FragmentRepository SELF;

    private FragmentRepository() {
    }

    public static FragmentRepository getInstance() {
        if (SELF != null) {
            return SELF;
        }
        synchronized (FragmentRepository.class) {
            if (SELF != null) {
                return SELF;
            }
            SELF = new FragmentRepository();
        }
        return SELF;
    }

    public Map<Integer, Fragment> fragmentMap = new HashMap<>();
    public int currentPos = 0;
}