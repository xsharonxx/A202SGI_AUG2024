package com.example.billsphere;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CBPayment extends AppCompatActivity {

    private ImageButton backButton;
    private Button doneButton;
    private TextView totalText;
    private RecyclerView recyclerView;

    private String customerId;
    private String totalAmount;

    private CBPaymentAdapter paymentAdapter;
    private List<String> paymentList;
    private String selectedPaymentMethod;

    public void setSelectedPaymentMethod(String method) {
        this.selectedPaymentMethod = method;
    }

    public interface OnAllProductsUploadedListener {
        void onAllUploaded();
    }

    private void uploadBillProductImage(String documentId, OnAllProductsUploadedListener listener){
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        int totalProducts = ProductCartManager.getInstance().getProductCount();
        AtomicInteger uploadCount = new AtomicInteger(0);

        for (CBProduct product : ProductCartManager.getInstance().getSelectedProducts()){

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference productRef = db.collection("product").document(product.getProductId());
            productRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Long currentStock = document.getLong("stock");

                            if (currentStock != null) {
                                long newStock = currentStock - product.getQuantity();
                                productRef.update("stock", newStock)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                StorageReference fileRef = storageRef.child("billImage/" + documentId + "/" + product.getProductId());

                                                Glide.with(getApplicationContext()).asBitmap().load(product.getProductImage())
                                                        .into(new CustomTarget<Bitmap>() {
                                                            @Override
                                                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                                                resource.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                                                byte[] data = baos.toByteArray();

                                                                fileRef.putBytes(data)
                                                                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                                                            @Override
                                                                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                                                fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                                                    @Override
                                                                                    public void onSuccess(Uri downloadUrl) {
                                                                                        product.setProductImage(String.valueOf(downloadUrl));

                                                                                        if (uploadCount.incrementAndGet() == totalProducts && listener != null) {
                                                                                            listener.onAllUploaded(); // Notify listener when all uploads are done
                                                                                        }
                                                                                    }
                                                                                });
                                                                            }
                                                                        });
                                                            }

                                                            @Override
                                                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                                            }
                                                        });
                                            }
                                        });
                            }
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.cb_payment);

        Intent intent = getIntent();
        customerId = intent.getStringExtra("CUSTOMER_ID");
        totalAmount = intent.getStringExtra("TOTAL_AMOUNT");

        selectedPaymentMethod = null;

        backButton = findViewById(R.id.back_button);
        doneButton = findViewById(R.id.done_button);
        totalText = findViewById(R.id.total_amount);
        recyclerView = findViewById(R.id.recycle_view);

        totalText.setText(totalAmount);

        recyclerView.setLayoutManager(new LinearLayoutManager(CBPayment.this));

        paymentList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.payment_methods)));

        paymentAdapter = new CBPaymentAdapter(paymentList, CBPayment.this);
        recyclerView.setAdapter(paymentAdapter);

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProgressDialog progressDialog = new ProgressDialog();
                progressDialog.show(getSupportFragmentManager(), "progressDialog");
                if (selectedPaymentMethod == null){
                    progressDialog.dismiss();
                    AlertDialog.Builder builder = new AlertDialog.Builder(CBPayment.this);
                    builder.setTitle("No Payment Method")
                            .setMessage("Please Select a Payment Method.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                } else {
                    DocumentReference counterRef = FirebaseFirestore.getInstance().collection("counter").document("bill");
                    counterRef.update("current", FieldValue.increment(1))
                            .addOnSuccessListener(aVoid -> {
                                        counterRef.get().addOnCompleteListener(counterTask -> {
                                            if (counterTask.isSuccessful()) {
                                                DocumentSnapshot document = counterTask.getResult();
                                                if (document.exists()) {
                                                    Long newId = document.getLong("current");

                                                    List<Map<String, Object>> productMaps = new ArrayList<>();

                                                    uploadBillProductImage(String.valueOf(newId), new OnAllProductsUploadedListener() {
                                                        @Override
                                                        public void onAllUploaded() {
                                                            for (CBProduct product : ProductCartManager.getInstance().getSelectedProducts()){
                                                                Map<String, Object> productMap = new HashMap<>();
                                                                productMap.put("productImage", product.getProductImage());
                                                                productMap.put("productName", product.getProductName());
                                                                productMap.put("productPrice", product.getProductPrice());
                                                                productMap.put("productCode", product.getProductCode());
                                                                productMap.put("categoryId", product.getCategoryId());
                                                                productMap.put("productId", product.getProductId());
                                                                productMap.put("quantity", product.getQuantity());
                                                                productMap.put("productProfit", product.getProductProfit());

                                                                productMaps.add(productMap);
                                                            }

                                                            SharedPreferences preferences1 = getSharedPreferences("user", MODE_PRIVATE);
                                                            String user = preferences1.getString("uid", "");

                                                            Locale locale = new Locale("ms", "MY");
                                                            Currency currency = Currency.getInstance(locale);
                                                            int maxDecimals = currency.getDefaultFractionDigits();

                                                            String numericString = totalAmount.replaceAll("[^\\d.]", "");
                                                            double numericDouble = Double.parseDouble(numericString);
                                                            BigDecimal roundedTotal = new BigDecimal(numericDouble).setScale(maxDecimals, RoundingMode.HALF_UP);
                                                            double totalData = roundedTotal.doubleValue();

                                                            Map<String, Object> billData = new HashMap<>();
                                                            billData.put("customerId", customerId);
                                                            billData.put("businessId", user);
                                                            billData.put("totalAmount", totalData);
                                                            billData.put("timestamp", FieldValue.serverTimestamp());
                                                            billData.put("paymentMethod", selectedPaymentMethod);
                                                            billData.put("selectedProducts", productMaps);

                                                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                                                            db.collection("bill")
                                                                    .document(String.valueOf(newId))
                                                                    .set(billData)
                                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                        @Override
                                                                        public void onSuccess(Void unused) {
                                                                            progressDialog.dismiss();
                                                                            Toast.makeText(getApplicationContext(), "Successful Create Bill", Toast.LENGTH_SHORT).show();
                                                                            ProductCartManager.getInstance().clearProducts();
                                                                            Intent intent = new Intent(CBPayment.this, BusinessActivity.class);
                                                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                                                            startActivity(intent);
                                                                            finish();
                                                                        }
                                                                    });
                                                        }
                                                    });
                                                }
                                            }
                                        });
                                    });
                }
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
