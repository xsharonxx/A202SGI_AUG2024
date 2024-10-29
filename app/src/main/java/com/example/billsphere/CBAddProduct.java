package com.example.billsphere;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CBAddProduct extends AppCompatActivity implements CBProductAdapter.OnSelectedProductUpdateListener{

    private ImageButton backButton;
    private SearchView searchBar;
    private ImageButton codeScanner;
    private RecyclerView recyclerView;
    private TextView cartCount;
    private Button nextButton;

    private String customerId;

    private CBProductAdapter productAdapter;
    private List<CBProduct> productList, filteredList;

    private void searchProduct(String s){
        filteredList.clear();

        if (s.isEmpty()) {
            // If the search bar is empty, return the full list
            filteredList.addAll(productList);
        } else {
            // Loop through your original list
            for (CBProduct product : productList) {
                if (product.getProductName().toLowerCase().contains(s.toLowerCase())
                        || product.getProductCode().toLowerCase().contains(s.toLowerCase())) {
                    filteredList.add(product);  // Add it to the filtered list
                }
            }
        }
        productAdapter.notifyDataSetChanged();
    }

    private void updateProductRecyclerView() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences preferences1 = getSharedPreferences("user", MODE_PRIVATE);
        String user = preferences1.getString("uid", "");
        CollectionReference productRef = db.collection("product");

        productRef.whereEqualTo("userId", user)
                .whereEqualTo("productStatus", true)
                .orderBy("productName", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            productList.clear();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String documentId = document.getId();
                                String productImage = document.getString("productImage");
                                String productName = document.getString("productName");
                                String productCode = document.getString("productCode");
                                String categoryId = document.getString("categoryId");
                                double productPrice = document.getDouble("price");
                                double productProfit = document.getDouble("profit");

                                productList.add(new CBProduct(productImage, productName, productPrice, categoryId,
                                        documentId, productProfit, productCode));
                            }

                            productAdapter.notifyDataSetChanged();

                            String currentSearchText = searchBar.getQuery().toString();
                            searchProduct(currentSearchText);
                        }
                    }
                });
    }

    @Override
    public void onSelectedProductUpdated() {
        int count = 0;
        for (CBProduct selectedProduct : ProductCartManager.getInstance().getSelectedProducts()) {
            count = count + selectedProduct.getQuantity();
        }
        if (count != 0) {
            cartCount.setText(String.valueOf(count));
            cartCount.setVisibility(View.VISIBLE);
        } else {
            cartCount.setText("0");
            cartCount.setVisibility(View.GONE);
        }
    }

    private void scanProductCode(){
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan a Product Bar Code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
        options.setCaptureActivity(CaptureAct.class);
        CodeScanLauncher.launch(options);
    }

    private final ActivityResultLauncher<ScanOptions> CodeScanLauncher = registerForActivityResult(new ScanContract(), result->{
        if(result.getContents() != null){
            String scannedProductCode = result.getContents();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            SharedPreferences preferences1 = getSharedPreferences("user", MODE_PRIVATE);
            String user = preferences1.getString("uid", "");

            db.collection("product")
                    .whereEqualTo("userId", user)
                    .whereEqualTo("productStatus", true)
                    .whereEqualTo("productCode", scannedProductCode)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                if (!task.getResult().isEmpty()) {
                                    searchBar.setQuery(scannedProductCode, true);

                                    DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                    String id = document.getId();
                                    String name = document.getString("productName");
                                    double price = document.getDouble("price");
                                    String image = document.getString("productImage");
                                    String categoryId = document.getString("categoryId");
                                    double profit = document.getDouble("profit");

                                    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(CBAddProduct.this);
                                    View bottomSheetView = getLayoutInflater().inflate(R.layout.cb_bottom_sheet, null);
                                    bottomSheetDialog.setContentView(bottomSheetView);

                                    TextView productName, productCategory, productCode, productPrice, totalAmount;
                                    ImageButton quantityMinusButton, quantityAddButton;
                                    EditText quantityField;
                                    Button addButton;

                                    productName = bottomSheetView.findViewById(R.id.product_name);
                                    productCategory = bottomSheetView.findViewById(R.id.category);
                                    productCode = bottomSheetView.findViewById(R.id.product_code);
                                    productPrice = bottomSheetView.findViewById(R.id.price);
                                    totalAmount = bottomSheetView.findViewById(R.id.total);
                                    quantityMinusButton = bottomSheetView.findViewById(R.id.quantity_minus);
                                    quantityAddButton = bottomSheetView.findViewById(R.id.quantity_add);
                                    quantityField = bottomSheetView.findViewById(R.id.quantity_field);
                                    addButton = bottomSheetView.findViewById(R.id.add_button);

                                    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                                    DocumentReference docRef = firestore.collection("category").document(categoryId);
                                    docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            if (documentSnapshot.exists()) {
                                                Map<String, Object> categoryData = documentSnapshot.getData();

                                                if (categoryData != null) {
                                                    String categoryName = (String) categoryData.get("categoryTitle");
                                                    productCategory.setText(categoryName);
                                                }
                                            }
                                        }
                                    });
                                    productName.setText(name);
                                    productCode.setText(scannedProductCode);
                                    Locale locale = new Locale("ms", "MY");
                                    NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
                                    String formattedPrice = currencyFormatter.format(price);
                                    productPrice.setText(formattedPrice);

                                    quantityField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                                        @Override
                                        public void onFocusChange(View v, boolean hasFocus) {
                                            if (hasFocus) {
                                                quantityField.setError(null);
                                            }
                                        }
                                    });

                                    quantityField.addTextChangedListener(new TextWatcher() {
                                        @Override
                                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                                        }

                                        @Override
                                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                                            quantityField.setError(null);
                                            String priceString = productPrice.getText().toString().trim();
                                            String numericPrice = priceString.replaceAll("[^\\d.]", "");
                                            String quantityString = quantityField.getText().toString().trim();
                                            if (!quantityString.isEmpty()){
                                                int quantity = Integer.parseInt(quantityString);

                                                if (quantity == 0) {
                                                    quantity = 1; // Set a minimum value of 1
                                                    quantityField.setText(String.valueOf(quantity));
                                                    quantityField.setSelection(quantityField.getText().length());
                                                }

                                                double price = Double.parseDouble(numericPrice);
                                                double total = price * quantity;
                                                String formattedTotal = currencyFormatter.format(total);
                                                totalAmount.setText(formattedTotal);
                                            } else {
                                                totalAmount.setText("RM0.00");
                                            }
                                        }

                                        @Override
                                        public void afterTextChanged(Editable editable) {
                                        }
                                    });

                                    quantityField.setText("1");

                                    quantityMinusButton.setOnClickListener(new View.OnClickListener(){
                                        @Override
                                        public void onClick(View view) {
                                            quantityField.setError(null);
                                            String quantityString = quantityField.getText().toString().trim();
                                            if (!quantityString.isEmpty()){
                                                int quantity = Integer.parseInt(quantityString);
                                                if (quantity > 1) {
                                                    quantity--;
                                                    quantityField.setText(String.valueOf(quantity));
                                                } else {
                                                    quantityField.setText("1");
                                                }
                                            } else {
                                                quantityField.setText("1");
                                            }
                                            quantityField.setSelection(quantityField.getText().length());
                                        }
                                    });

                                    quantityAddButton.setOnClickListener(new View.OnClickListener(){
                                        @Override
                                        public void onClick(View view) {
                                            quantityField.setError(null);
                                            String quantityString = quantityField.getText().toString().trim();
                                            if (!quantityString.isEmpty()){
                                                int quantity = Integer.parseInt(quantityString);
                                                if (quantity >= 1) {
                                                    quantity++;
                                                    quantityField.setText(String.valueOf(quantity));
                                                } else {
                                                    quantityField.setText("1");
                                                }
                                            } else {
                                                quantityField.setText("1");
                                            }
                                            quantityField.setSelection(quantityField.getText().length());
                                        }
                                    });

                                    addButton.setOnClickListener(new View.OnClickListener(){
                                        @Override
                                        public void onClick(View view) {
                                            String quantityString = quantityField.getText().toString().trim();
                                            if (!quantityString.isEmpty()){
                                                quantityField.setError(null);

                                                int quantityInt = Integer.parseInt(quantityString);
                                                boolean productExists = false;

                                                for (CBProduct selectedProduct : ProductCartManager.getInstance().getSelectedProducts()) {
                                                    if (selectedProduct.getProductCode().equals(scannedProductCode)) {
                                                        selectedProduct.setQuantity(selectedProduct.getQuantity() + quantityInt);
                                                        productExists = true;
                                                        break;
                                                    }
                                                }

                                                if (!productExists){
                                                    CBProduct scannedProduct = new CBProduct(image, name, price, categoryId, id, profit, scannedProductCode);
                                                    scannedProduct.setQuantity(quantityInt);
                                                    ProductCartManager.getInstance().addProduct(scannedProduct);
                                                }
                                                onSelectedProductUpdated();
                                                bottomSheetDialog.dismiss();
                                            } else {
                                                quantityField.setError("Required*");
                                            }
                                        }
                                    });

                                    bottomSheetDialog.show();
                                } else {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(CBAddProduct.this);
                                    builder.setTitle("Error");
                                    builder.setMessage("Invalid Bar Code");
                                    builder.setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.dismiss();
                                            scanProductCode();
                                        }
                                    });
                                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.dismiss();
                                        }
                                    });
                                    builder.show();
                                }
                            }
                        }
                    });
        }
    });

    @Override
    public void onResume() {
        super.onResume();

        onSelectedProductUpdated();
    }

    @Override
    public void onBackPressed() {
        ProductCartManager.getInstance().clearProducts();
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.cb_add_product);

        Intent intent = getIntent();
        customerId = intent.getStringExtra("CUSTOMER_ID");

        backButton = findViewById(R.id.back_button);
        searchBar = findViewById(R.id.search_bar);
        codeScanner = findViewById(R.id.qr_scanner_button);
        recyclerView = findViewById(R.id.recycle_view);
        cartCount = findViewById(R.id.cart_count);
        nextButton = findViewById(R.id.next_button);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2); // 2 columns
        recyclerView.setLayoutManager(gridLayoutManager);

        productList = new ArrayList<>();
        filteredList = new ArrayList<>();

        productAdapter = new CBProductAdapter(filteredList, CBAddProduct.this, ProductCartManager.getInstance().getSelectedProducts(), this);
        recyclerView.setAdapter(productAdapter);

        updateProductRecyclerView();

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProductCartManager.getInstance().clearProducts();
                finish();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int productQuantity = 0;
                productQuantity = Integer.parseInt(cartCount.getText().toString().trim());
                if (productQuantity != 0) {
                    Intent intent = new Intent(CBAddProduct.this, CBCart.class);
                    intent.putExtra("CUSTOMER_ID", customerId);
                    startActivity(intent);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(CBAddProduct.this);
                    builder.setTitle("Empty Cart")
                            .setMessage("Cart is empty. Please add products before proceeding.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                }
            }
        });

        codeScanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View focusedView = getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(CBAddProduct.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }
                scanProductCode();
            }
        });

        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                View focusedView = getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(MPManageCategory.INPUT_METHOD_SERVICE);
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
    }
}
