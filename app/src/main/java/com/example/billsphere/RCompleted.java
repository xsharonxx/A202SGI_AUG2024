package com.example.billsphere;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RCompleted extends Fragment implements RequestAdapter.OnRequestUpdateListener{

    private SearchView searchBar;
    private ImageButton filterButton;
    private RecyclerView recyclerView;

    private RequestAdapter requestAdapter;

    private List<Request> requestList, filteredList;

    private final int[] selectedFilter = {0, 0};

    @Override
    public void OnRequestUpdated(){
        updateRequest();
    }

    private void updateRequest(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference requestRef = db.collection("request");

        Query query = requestRef.whereNotEqualTo("acceptRejectTime", null);

        if (selectedFilter[0] == 0) {
            query = query.orderBy("acceptRejectTime", Query.Direction.DESCENDING);
        } else {
            query = query.orderBy("acceptRejectTime", Query.Direction.ASCENDING);
        }

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    requestList.clear();

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String requestId = document.getId();
                        String userId = document.getString("userId");

                        Timestamp timestamp = document.getTimestamp("requestTime");
                        LocalDateTime localDateTime = null;
                        if (timestamp != null) {
                            localDateTime = timestamp.toDate()
                                    .toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime();
                        }

                        String name = document.getString("businessName");
                        String email = document.getString("businessEmail");
                        String rejectReason = document.getString("rejectReason");
                        Timestamp acceptRejectTimestamp = document.getTimestamp("acceptRejectTime");
                        LocalDateTime acceptRejectLocalDateTime = null;
                        if (acceptRejectTimestamp != null) {
                            acceptRejectLocalDateTime = acceptRejectTimestamp.toDate()
                                    .toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime();
                        }

                        Request temp = new Request(requestId, userId, localDateTime);
                        temp.setUserEmail(email);
                        temp.setUserName(name);
                        temp.setRejectReason(rejectReason);
                        temp.setAcceptRejectTimestamp(acceptRejectLocalDateTime);

                        if (rejectReason == null){
                            temp.setBusinessStatus("Accepted");
                        } else {
                            temp.setBusinessStatus("Rejected");
                        }

                        requestList.add(temp);
                    }

                    List<Request> filteredRequests = new ArrayList<>();

                    if (selectedFilter[1] == 1) {
                        for (Request request : requestList) {
                            if (request.getBusinessStatus().equals("Accepted")) {
                                filteredRequests.add(request);
                            }
                        }
                    } else if (selectedFilter[1] == 2){
                        for (Request request : requestList) {
                            if (request.getBusinessStatus().equals("Rejected")) {
                                filteredRequests.add(request);
                            }
                        }
                    } else {
                        filteredRequests.addAll(requestList);
                    }

                    requestList.clear();
                    requestList.addAll(filteredRequests);
                    requestAdapter.notifyDataSetChanged();
                    String currentSearchText = searchBar.getQuery().toString();
                    searchRequest(currentSearchText);
                }
            }
        });
    }

    private void showFilterDialog(){
        String[] sortOptions = {"Newest -> Oldest", "Oldest -> Newest"};
        String[] sortOptions2 = {"Both", "Accepted", "Rejected"};

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Filter");
        View dialogView = getLayoutInflater().inflate(R.layout.r_completed_filter, null);
        builder.setView(dialogView);
        RadioGroup sortRadioGroup = dialogView.findViewById(R.id.sort_radio_group);
        RadioGroup radioGroup = dialogView.findViewById(R.id.radio_group);

        for (int i = 0; i < sortOptions.length; i++) {
            RadioButton radioButton = new RadioButton(requireContext());
            radioButton.setText(sortOptions[i]);
            radioButton.setId(i);  // Assign an ID to each RadioButton
            sortRadioGroup.addView(radioButton);
        }
        sortRadioGroup.check(selectedFilter[0]);

        for (int i = 0; i < sortOptions2.length; i++) {
            RadioButton radioButton = new RadioButton(requireContext());
            radioButton.setText(sortOptions2[i]);
            radioButton.setId(i);
            radioGroup.addView(radioButton);
        }
        radioGroup.check(selectedFilter[1]);

        builder.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Get option
                int selectedSortId = sortRadioGroup.getCheckedRadioButtonId();
                selectedFilter[0] = selectedSortId;

                int selectedId = radioGroup.getCheckedRadioButtonId();
                selectedFilter[1] = selectedId;

                updateRequest();
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("Cancel", null);

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void searchRequest(String s){
        filteredList.clear();

        if (s.isEmpty()) {
            filteredList.addAll(requestList);
        } else {
            for (Request request : requestList) {
                if (request.getRequestId().toLowerCase().contains(s.toLowerCase())
                        ||request.getUserName().toLowerCase().contains(s.toLowerCase())
                        ||request.getUserEmail().toLowerCase().contains(s.toLowerCase())) {
                    filteredList.add(request);  // Add it to the filtered list
                }
            }
        }
        requestAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();

        updateRequest();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.requests, container, false);

        searchBar = view.findViewById(R.id.search_bar);
        filterButton = view.findViewById(R.id.filter_button);
        recyclerView = view.findViewById(R.id.recycle_view);

        requestList = new ArrayList<>();
        filteredList = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        requestAdapter = new RequestAdapter(filteredList, this, this);
        recyclerView.setAdapter(requestAdapter);

        updateRequest();

        filterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View focusedView = getView().findFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }
                showFilterDialog();
            }
        });

        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                View focusedView = getView().findFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                searchRequest(s);
                return false;
            }
        });

        return view;
    }
}
