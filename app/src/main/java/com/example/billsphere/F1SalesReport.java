package com.example.billsphere;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class F1SalesReport extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private SRViewPageAdapter viewPageAdapter;

    private TextView mainText, secondText, thirdText;
    private Button goToProfileButton;

    private void updatePendingRejected(String status){
        if (status.equals("rejected")){
            mainText.setText(R.string.request_rejected);
            secondText.setText(R.string.check_right_resubmit);
            thirdText.setText(R.string.reject_reason);
            thirdText.setFocusable(true);
            thirdText.setClickable(true);
            thirdText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    SharedPreferences preferences1 = getActivity().getSharedPreferences("user", Context.MODE_PRIVATE);
                    String user = preferences1.getString("uid", "");
                    db.collection("request")
                            .whereEqualTo("userId", user)
                            .orderBy("acceptRejectTime", Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                    DocumentSnapshot latestRequest = task.getResult().getDocuments().get(0);
                                    String rejectReason = latestRequest.getString("rejectReason");
                                    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
                                    View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_reject_reason, null);
                                    bottomSheetDialog.setContentView(bottomSheetView);
                                    TextView bottomSheetTitle = bottomSheetView.findViewById(R.id.bottom_sheet_title);
                                    TextView bottomSheetInput = bottomSheetView.findViewById(R.id.bottom_sheet_text_input);
                                    bottomSheetTitle.setText("Reject Reason");
                                    bottomSheetInput.setText(rejectReason);
                                    bottomSheetDialog.show();
                                }
                            });
                }
            });
        } else if (status.equals("pending")){
            mainText.setText(R.string.request_in_progress);
            secondText.setText(R.string.take3_5business_day);
            thirdText.setText(R.string.more_details);
            thirdText.setFocusable(false);
            thirdText.setClickable(false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view;
        SharedPreferences preferences = getActivity().getSharedPreferences("businessStatus", Context.MODE_PRIVATE);
        String status = preferences.getString("status", "");

        if (status.equals("accepted")) {
            view = inflater.inflate(R.layout.fragment1_sales_report, container, false);

            tabLayout = view.findViewById(R.id.tab_layout);
            viewPager = view.findViewById(R.id.view_pager);

            viewPageAdapter = new SRViewPageAdapter(getActivity());
            viewPager.setAdapter(viewPageAdapter);

            new TabLayoutMediator(tabLayout, viewPager, new TabLayoutMediator.TabConfigurationStrategy() {
                @Override
                public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                    switch (position) {
                        case 0:
                            tab.setText("Overall");
                            break;
                        case 1:
                            tab.setText("Bills");
                            break;
                    }
                }
            }).attach();
        } else {
            view = inflater.inflate(R.layout.fragment_pending_reject, container, false);

            mainText = view.findViewById(R.id.main_text);
            secondText = view.findViewById(R.id.second_text);
            thirdText = view.findViewById(R.id.third_text);
            goToProfileButton = view.findViewById(R.id.go_to_profile_button);

            updatePendingRejected(status);

            goToProfileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), ProfileBusiness.class);
                    startActivity(intent);
                }
            });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences preferences = getActivity().getSharedPreferences("businessStatus", Context.MODE_PRIVATE);
        String status = preferences.getString("status", "");
        if (!status.equals("accepted")) {
            updatePendingRejected(status);
        }
    }
}
