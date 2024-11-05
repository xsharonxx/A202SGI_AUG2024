package com.example.billsphere;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class RequestsViewPageAdapter extends FragmentStateAdapter {

    public RequestsViewPageAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new RPending(); // First Tab
            case 1:
                return new RCompleted(); // Second Tab
            default:
                return new RPending(); // Default (just in case)
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Number of tabs
    }
}
