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
import java.util.HashMap;
import java.util.List;

public class RPending extends Fragment implements RequestAdapter.OnRequestUpdateListener{

    private SearchView searchBar;
    private ImageButton filterButton;
    private RecyclerView recyclerView;

    private RequestAdapter requestAdapter;

    private List<Request> requestList, filteredList;

    private final int[] selectedFilter = {0};

    @Override
    public void OnRequestUpdated(){
        updateRequest();
    }

    private void updateRequest(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference requestRef = db.collection("request");

        if (selectedFilter[0] == 0){
            requestRef.whereEqualTo("acceptRejectTime", null)
                    .orderBy("requestTime", Query.Direction.ASCENDING)
                    .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()){
                                requestList.clear();
                                List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();

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
                                    Request temp = new Request(requestId, userId, localDateTime);
                                    Task<DocumentSnapshot> userTask = db.collection("user").document(userId).get();

                                    userTask.addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            String name = userDoc.getString("userName");
                                            String email = userDoc.getString("userEmail");
                                            temp.setUserName(name);
                                            temp.setUserEmail(email);
                                        }
                                    });
                                    userTasks.add(userTask);
                                    requestList.add(temp);
                                }

                                Tasks.whenAllComplete(userTasks).addOnCompleteListener(taskList -> {
                                    Collections.sort(requestList, (r1, r2) -> {
                                        LocalDateTime time2 = r2.getRequestTimestamp();
                                        LocalDateTime time1 = r1.getRequestTimestamp();
                                        return time1.compareTo(time2); // Sort in ascending order
                                    });
                                    requestAdapter.notifyDataSetChanged();
                                    String currentSearchText = searchBar.getQuery().toString();
                                    searchRequest(currentSearchText);
                                });
                            }
                        }
                    });
        } else {
            requestRef.whereEqualTo("acceptRejectTime", null)
                    .orderBy("requestTime", Query.Direction.DESCENDING)
                    .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                requestList.clear();
                                List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();

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
                                    Request temp = new Request(requestId, userId, localDateTime);
                                    Task<DocumentSnapshot> userTask = db.collection("user").document(userId).get();

                                    userTask.addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            String name = userDoc.getString("userName");
                                            String email = userDoc.getString("userEmail");
                                            temp.setUserName(name);
                                            temp.setUserEmail(email);
                                        }
                                    });
                                    userTasks.add(userTask);
                                    requestList.add(temp);
                                }

                                Tasks.whenAllComplete(userTasks).addOnCompleteListener(taskList -> {
                                    Collections.sort(requestList, (r1, r2) -> {
                                        LocalDateTime time2 = r2.getRequestTimestamp();
                                        LocalDateTime time1 = r1.getRequestTimestamp();
                                        return time2.compareTo(time1); // Sort in descending order
                                    });
                                    requestAdapter.notifyDataSetChanged();
                                    String currentSearchText = searchBar.getQuery().toString();
                                    searchRequest(currentSearchText);
                                });
                            }
                        }
                    });
        }
    }

    private void showFilterDialog(){
        String[] filterOptions = {"Oldest -> Newest", "Newest -> Oldest"};

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Filter");

        // Set up the single-choice items in the dialog
        builder.setSingleChoiceItems(filterOptions, selectedFilter[0], new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedFilter[0] = which;
                updateRequest();
                dialog.dismiss();
            }
        });

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
