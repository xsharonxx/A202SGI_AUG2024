package com.example.billsphere;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class SRViewPageAdapter extends FragmentStateAdapter {

    public SRViewPageAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new SROverall(); // First Tab
            case 1:
                return new SRBills(); // Second Tab
            default:
                return new SROverall(); // Default (just in case)
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Number of tabs
    }
}
