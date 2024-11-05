package com.example.billsphere;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class F1ManageProducts extends Fragment{

    private Button manageCategoryButton, addProductButton;
    private SearchView searchBar;
    private ImageButton filterButton;
    private RecyclerView recyclerView;

    private TextView mainText, secondText, thirdText;
    private Button goToProfileButton;

    private ProductAdapter productAdapter;
    private List<Product> productList, filteredList;

    private final int[] selectedFilter = {0, 0};

    private void updateProductRecyclerView(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences preferences1 = getActivity().getSharedPreferences("user", Context.MODE_PRIVATE);
        String user = preferences1.getString("uid", "");
        CollectionReference productRef = db.collection("product");

        Query.Direction orderDirection = null;
        if (selectedFilter[0] == 0){
            orderDirection = Query.Direction.ASCENDING;
        } else if (selectedFilter[0] == 1) {
            orderDirection = Query.Direction.DESCENDING;
        }

        if (selectedFilter[1] == 0) {
            productRef.whereEqualTo("userId", user)
                    .orderBy("productStatus", Query.Direction.DESCENDING)
                    .orderBy("productName", orderDirection)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                productList.clear();

                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String documentId = document.getId();
                                    String productName = document.getString("productName");
                                    String productCode = document.getString("productCode");
                                    String productImage = document.getString("productImage");
                                    String categoryId = document.getString("categoryId");
                                    boolean productStatus = document.getBoolean("productStatus");
                                    int stock = document.getLong("stock").intValue();
                                    double cost = document.getDouble("cost");
                                    double price = document.getDouble("price");
                                    double profit = document.getDouble("profit");

                                    productList.add(new Product(productImage, productName, productCode, categoryId,
                                            stock, cost, price, profit, productStatus, documentId));
                                }

                                productAdapter.notifyDataSetChanged();

                                String currentSearchText = searchBar.getQuery().toString();
                                searchProduct(currentSearchText);
                            }
                        }
                    });
        } else {
            String categoryId = String.valueOf(selectedFilter[1]);
            productRef.whereEqualTo("userId", user)
                    .whereEqualTo("categoryId", categoryId)
                    .orderBy("productStatus", Query.Direction.DESCENDING)
                    .orderBy("productName", orderDirection)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                productList.clear();

                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String documentId = document.getId();
                                    String productName = document.getString("productName");
                                    String productCode = document.getString("productCode");
                                    String productImage = document.getString("productImage");
                                    String categoryId = document.getString("categoryId");
                                    boolean productStatus = document.getBoolean("productStatus");
                                    int stock = document.getLong("stock").intValue();
                                    double cost = document.getDouble("cost");
                                    double price = document.getDouble("price");
                                    double profit = document.getDouble("profit");

                                    productList.add(new Product(productImage, productName, productCode, categoryId,
                                            stock, cost, price, profit, productStatus, documentId));
                                }

                                productAdapter.notifyDataSetChanged();

                                String currentSearchText = searchBar.getQuery().toString();
                                searchProduct(currentSearchText);
                            }
                        }
                    });
        }
    }

    private void searchProduct(String s){
        filteredList.clear();

        if (s.isEmpty()) {
            filteredList.addAll(productList);
        } else {
            for (Product product : productList) {
                if (product.getProductName().toLowerCase().contains(s.toLowerCase()) ||
                        product.getProductCode().toLowerCase().contains(s.toLowerCase())) {
                    filteredList.add(product);  // Add it to the filtered list
                }
            }
        }
        productAdapter.notifyDataSetChanged();
    }

    private void showFilterDialog() {
        String[] sortOptions = {"A to Z", "Z to A"};

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences preferences1 = getActivity().getSharedPreferences("user", Context.MODE_PRIVATE);
        String user = preferences1.getString("uid", "");

        db.collection("category").whereEqualTo("userId", user)
                .orderBy("categoryTitle", Query.Direction.ASCENDING)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> categoryNames = new ArrayList<>();
                        List<String> categoryIds = new ArrayList<>();

                        categoryNames.add("All Categories");
                        categoryIds.add("0");

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String categoryName = document.getString("categoryTitle");
                            String categoryId = document.getId();
                            categoryNames.add(categoryName);
                            categoryIds.add(categoryId);
                        }

                        // Build the dialog
                        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                        builder.setTitle("Select Filter");
                        View dialogView = getLayoutInflater().inflate(R.layout.mp_filter_radiobutton, null);
                        builder.setView(dialogView);
                        RadioGroup sortRadioGroup = dialogView.findViewById(R.id.sort_radio_group);
                        RadioGroup categoryRadioGroup = dialogView.findViewById(R.id.category_radio_group);

                        for (int i = 0; i < sortOptions.length; i++) {
                            RadioButton radioButton = new RadioButton(requireContext());
                            radioButton.setText(sortOptions[i]);
                            radioButton.setId(i);  // Assign an ID to each RadioButton
                            sortRadioGroup.addView(radioButton);
                        }
                        sortRadioGroup.check(selectedFilter[0]);

                        for (int i = 0; i < categoryNames.size(); i++) {
                            RadioButton radioButton = new RadioButton(requireContext());
                            radioButton.setText(categoryNames.get(i));
                            radioButton.setId(Integer.parseInt(categoryIds.get(i)));
                            categoryRadioGroup.addView(radioButton);
                        }
                        categoryRadioGroup.check(selectedFilter[1]);

                        builder.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Get selected sort option
                                int selectedSortId = sortRadioGroup.getCheckedRadioButtonId();
                                selectedFilter[0] = selectedSortId;

                                // Get selected category filter
                                int selectedCategoryId = categoryRadioGroup.getCheckedRadioButtonId();
                                selectedFilter[1] = selectedCategoryId;

                                updateProductRecyclerView();
                                dialog.dismiss();
                            }
                        });

                        builder.setNegativeButton("Cancel", null);

                        // Show the dialog
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                });
    }

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
            view = inflater.inflate(R.layout.fragment1_manage_products, container, false);
            manageCategoryButton = view.findViewById(R.id.manage_category_button);
            addProductButton = view.findViewById(R.id.add_product_button);
            searchBar = view.findViewById(R.id.search_bar);
            filterButton = view.findViewById(R.id.filter_button);
            recyclerView = view.findViewById(R.id.recycle_view);

            // Initialize RecyclerView
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

            // Initialize Item List
            productList = new ArrayList<>();
            filteredList = new ArrayList<>();

            // Initialize Adapter
            productAdapter = new ProductAdapter(filteredList, this);
            recyclerView.setAdapter(productAdapter);

            updateProductRecyclerView();

            manageCategoryButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), MPManageCategory.class);
                    startActivity(intent);
                }
            });

            addProductButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), MPAddProduct.class);
                    startActivity(intent);
                }
            });

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
                    searchProduct(s);
                    return false;
                }
            });
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
        if (status.equals("accepted")) {
            updateProductRecyclerView();
        } else {
            updatePendingRejected(status);
        }
    }
}
