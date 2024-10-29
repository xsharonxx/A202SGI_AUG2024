package com.example.billsphere;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class SViewDetailAdmin extends AppCompatActivity {

    private String businessId;
    private ImageButton backButton;
    private TextView businessName, businessEmail, businessContact, businessAddress;
    private ImageView businessImage;
    private ImageButton filterButton;
    private SearchView searchBar;
    private RecyclerView recyclerView;
    private TextView businessDocText, ownerDocText;
    private ImageButton businessDocButton, ownerDocButton;

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

        Query query;
        if (selectedFilter[1] == 0) {
            query = productRef.whereEqualTo("userId", businessId)
                    .orderBy("productStatus", Query.Direction.DESCENDING)
                    .orderBy("productName", orderDirection);
        } else {
            String categoryId = String.valueOf(selectedFilter[1]);
            query = productRef.whereEqualTo("userId", businessId)
                    .whereEqualTo("categoryId", categoryId)
                    .orderBy("productStatus", Query.Direction.DESCENDING)
                    .orderBy("productName", orderDirection);
        }

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
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
                        boolean productStatus = document.getBoolean("productStatus");

                        productList.add(new SProduct(productImage, productName, price, categoryId,
                                documentId, productCode, productStatus));
                    }

                    productAdapter.notifyDataSetChanged();

                    String currentSearchText = searchBar.getQuery().toString();
                    searchProduct(currentSearchText);
                }
            }
        });
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

    private void openPdf(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private void downloadAndOpenPDF(String proofUrl, String fileName) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReferenceFromUrl(proofUrl);

        storageReference.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // File downloaded successfully, now store it in Downloads
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                ContentResolver resolver = getContentResolver();
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    try {
                        OutputStream outputStream = resolver.openOutputStream(uri);
                        if (outputStream != null) {
                            outputStream.write(bytes);
                            outputStream.close();
                            Toast.makeText(getApplicationContext(), "Download successful", Toast.LENGTH_SHORT).show();
                            openPdf(uri);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Failed to save PDF", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.s_view_detail_admin);

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
        businessDocText = findViewById(R.id.business_doc_text);
        businessDocButton = findViewById(R.id.business_doc_button);
        ownerDocText = findViewById(R.id.owner_doc_text);
        ownerDocButton = findViewById(R.id.owner_doc_button);

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
                                String businessDocPath = document.getString("businessProofUrl");
                                String ownerDocPath = document.getString("ownerProofUrl");

                                if (image != null) {
                                    Glide.with(SViewDetailAdmin.this).load(image)
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

                                String businessWithoutParams = businessDocPath.split("\\?")[0];
                                String businessDecodedUrl = Uri.decode(businessWithoutParams);
                                String businessFileName = businessDecodedUrl.substring(businessDecodedUrl.lastIndexOf("/") + 1);
                                businessDocText.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        View focusedView = getCurrentFocus();
                                        if (focusedView != null){
                                            focusedView.clearFocus();
                                            InputMethodManager imm = (InputMethodManager) getSystemService(SViewDetailAdmin.INPUT_METHOD_SERVICE);
                                            if (imm != null) {
                                                imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                                            }
                                        }
                                        downloadAndOpenPDF(businessDocPath, businessFileName);
                                    }
                                });
                                businessDocButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        View focusedView = getCurrentFocus();
                                        if (focusedView != null){
                                            focusedView.clearFocus();
                                            InputMethodManager imm = (InputMethodManager) getSystemService(SViewDetailAdmin.INPUT_METHOD_SERVICE);
                                            if (imm != null) {
                                                imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                                            }
                                        }
                                        downloadAndOpenPDF(businessDocPath, businessFileName);
                                    }
                                });

                                String ownerWithoutParams = ownerDocPath.split("\\?")[0];
                                String ownerDecodedUrl = Uri.decode(ownerWithoutParams);
                                String ownerFileName = ownerDecodedUrl.substring(ownerDecodedUrl.lastIndexOf("/") + 1);
                                ownerDocText.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        View focusedView = getCurrentFocus();
                                        if (focusedView != null){
                                            focusedView.clearFocus();
                                            InputMethodManager imm = (InputMethodManager) getSystemService(SViewDetailAdmin.INPUT_METHOD_SERVICE);
                                            if (imm != null) {
                                                imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                                            }
                                        }
                                        downloadAndOpenPDF(ownerDocPath, ownerFileName);
                                    }
                                });
                                ownerDocButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        View focusedView = getCurrentFocus();
                                        if (focusedView != null){
                                            focusedView.clearFocus();
                                            InputMethodManager imm = (InputMethodManager) getSystemService(SViewDetailAdmin.INPUT_METHOD_SERVICE);
                                            if (imm != null) {
                                                imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                                            }
                                        }
                                        downloadAndOpenPDF(ownerDocPath, ownerFileName);
                                    }
                                });
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
                    InputMethodManager imm = (InputMethodManager) getSystemService(SViewDetailAdmin.INPUT_METHOD_SERVICE);
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
                    InputMethodManager imm = (InputMethodManager) getSystemService(SViewDetailAdmin.INPUT_METHOD_SERVICE);
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
