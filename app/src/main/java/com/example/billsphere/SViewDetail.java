package com.example.billsphere;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class SViewDetail extends AppCompatActivity {

    private String businessId;
    private ImageButton backButton;
    private TextView businessName, businessEmail, businessContact, businessAddress;
    private ImageView businessImage;
    private ImageButton filterButton;
    private SearchView searchBar;
    private RecyclerView recyclerView;

    private SProductAdapter productAdapter;

    private List<SProduct> productList, filteredList;

    private final int[] selectedFilter = {0, 0};

    private void updateProductRecyclerView(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference productRef = db.collection("product");

        Query.Direction orderDirection = null;
        if (selectedFilter[0] == 0){
            orderDirection = Query.Direction.ASCENDING;
        } else if (selectedFilter[0] == 1) {
            orderDirection = Query.Direction.DESCENDING;
        }

        if (selectedFilter[1] == 0) {
            productRef.whereEqualTo("userId", businessId)
                    .whereEqualTo("productStatus", true)
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
                                    double price = document.getDouble("price");

                                    productList.add(new SProduct(productImage, productName, price, categoryId,
                                            documentId, productCode, true));
                                }

                                productAdapter.notifyDataSetChanged();

                                String currentSearchText = searchBar.getQuery().toString();
                                searchProduct(currentSearchText);
                            }
                        }
                    });
        } else {
            String categoryId = String.valueOf(selectedFilter[1]);
            productRef.whereEqualTo("userId", businessId)
                    .whereEqualTo("categoryId", categoryId)
                    .whereEqualTo("productStatus", true)
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
                                    double price = document.getDouble("price");

                                    productList.add(new SProduct(productImage, productName, price, categoryId,
                                            documentId, productCode, true));
                                }

                                productAdapter.notifyDataSetChanged();

                                String currentSearchText = searchBar.getQuery().toString();
                                searchProduct(currentSearchText);
                            }
                        }
                    });
        }
    }

    private void showFilterDialog(){
        String[] sortOptions = {"A to Z", "Z to A"};

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("category").whereEqualTo("userId", businessId)
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
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Select Filter");
                        View dialogView = getLayoutInflater().inflate(R.layout.mp_filter_radiobutton, null);
                        builder.setView(dialogView);
                        RadioGroup sortRadioGroup = dialogView.findViewById(R.id.sort_radio_group);
                        RadioGroup categoryRadioGroup = dialogView.findViewById(R.id.category_radio_group);

                        for (int i = 0; i < sortOptions.length; i++) {
                            RadioButton radioButton = new RadioButton(this);
                            radioButton.setText(sortOptions[i]);
                            radioButton.setId(i);  // Assign an ID to each RadioButton
                            sortRadioGroup.addView(radioButton);
                        }
                        sortRadioGroup.check(selectedFilter[0]);

                        for (int i = 0; i < categoryNames.size(); i++) {
                            RadioButton radioButton = new RadioButton(this);
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

    private void searchProduct(String s){
        filteredList.clear();

        if (s.isEmpty()) {
            filteredList.addAll(productList);
        } else {
            for (SProduct product : productList) {
                if (product.getProductName().toLowerCase().contains(s.toLowerCase()) ||
                        product.getProductCode().toLowerCase().contains(s.toLowerCase())) {
                    filteredList.add(product);  // Add it to the filtered list
                }
            }
        }
        productAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.s_view_detail);

        Intent intent = getIntent();
        businessId = intent.getStringExtra("BUSINESS_ID");

        backButton = findViewById(R.id.back_button);
        businessName = findViewById(R.id.business_name);
        businessEmail = findViewById(R.id.business_email);
        businessContact = findViewById(R.id.business_contact);
        businessAddress = findViewById(R.id.business_address);
        businessImage = findViewById(R.id.business_image);
        filterButton = findViewById(R.id.filter_button);
        searchBar = findViewById(R.id.search_bar);
        recyclerView = findViewById(R.id.recycle_view);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2); // 2 columns
        recyclerView.setLayoutManager(gridLayoutManager);

        productList = new ArrayList<>();
        filteredList = new ArrayList<>();

        productAdapter = new SProductAdapter(filteredList, this);
        recyclerView.setAdapter(productAdapter);

        updateProductRecyclerView();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("user")
                .document(businessId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String image = document.getString("userImage");
                                String name = document.getString("userName");
                                String address = document.getString("userAddress");
                                String pos = document.getString("userPosCode");
                                String country = document.getString("userCountry");
                                String email = document.getString("userEmail");
                                String dial = document.getString("userDialCode");
                                String contact = document.getString("userContact");

                                if (image != null) {
                                    Glide.with(SViewDetail.this).load(image)
                                            .apply(RequestOptions.circleCropTransform()).into(businessImage);
                                } else {
                                    businessImage.setImageResource(R.drawable.null_profile_image);
                                }
                                businessName.setText(name);
                                businessEmail.setText(email);
                                String fullAddress = address + ", " + pos + ", " + country;
                                businessAddress.setText(fullAddress);
                                String dialCode = dial.split(" ")[0];
                                String fullContact = dialCode + " " + contact;
                                businessContact.setText(fullContact);
                            }
                        }
                    }
                });

        filterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View focusedView = getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(SViewDetail.INPUT_METHOD_SERVICE);
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
                View focusedView = getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(SViewDetail.INPUT_METHOD_SERVICE);
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

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}
