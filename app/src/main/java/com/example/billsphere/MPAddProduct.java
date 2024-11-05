package com.example.billsphere;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;


public class MPAddProduct extends AppCompatActivity{

    private ImageButton backButton;
    private ImageView productImage;
    private TextView productImageText;
    private TextInputLayout categoryLayout;
    private AutoCompleteTextView categoryInput;
    private TextInputLayout productNameLayout, productCodeLayout, stockLayout, costLayout, priceLayout;
    private TextInputEditText productNameInput, productCodeInput, stockInput, costInput, priceInput;
    private TextView profitText;
    private Button doneButton;

    private ArrayAdapter<String> categoryArrayAdapter;

    private File productImageFile;
    private double profitData;

    // Modify the layout
    private void setLayoutError(TextInputLayout layout, String message){
        if (message == null || message.isEmpty()) {
            // No error
            layout.setError(null);
            layout.setErrorEnabled(false);
        } else {
            //With error
            layout.setErrorEnabled(true);
            layout.setError(message);
        }
    }

    private File saveFileToTempStorage(Uri fileUri) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileUri.toString());
        File tempFile = new File(getCacheDir(), "tempFile." + extension);

        try (InputStream inputStream = getContentResolver().openInputStream(fileUri);
             OutputStream outputStream = new FileOutputStream(tempFile)) {
            if (inputStream != null) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return tempFile;
    }

    private final ActivityResultLauncher<Intent> pickProductImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        Glide.with(this).load(fileUri).apply(RequestOptions.centerCropTransform()).into(productImage);
                        productImageFile = saveFileToTempStorage(fileUri);
                        productImageText.setVisibility(View.GONE);
                    }
                }
            });

    private void uploadProductImage(){
        ImagePicker.with(this).cropSquare().compress(512).maxResultSize(512, 512)
                .createIntent(new Function1<Intent, Unit>() {
                    @Override
                    public Unit invoke(Intent intent) {
                        pickProductImageLauncher.launch(intent);
                        return null;
                    }
                });
    }

    private void addNewProduct(){
        ProgressDialog progressDialog = new ProgressDialog();
        progressDialog.show(getSupportFragmentManager(), "progressDialog");
        String category = categoryInput.getText().toString().trim();
        String name = productNameInput.getText().toString().trim();
        String code = productCodeInput.getText().toString().trim();
        String stock = stockInput.getText().toString().trim();
        String cost = costInput.getText().toString().trim();
        String price = priceInput.getText().toString().trim();

        boolean valid = true;

        if (name.isEmpty()) {
            setLayoutError(productNameLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(productNameLayout, null);
        }
        if (code.isEmpty()) {
            setLayoutError(productCodeLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(productCodeLayout, null);
        }
        if (stock.isEmpty()) {
            setLayoutError(stockLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(stockLayout, null);
        }
        if (cost.isEmpty()) {
            setLayoutError(costLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(costLayout, null);
        }
        if (price.isEmpty()) {
            setLayoutError(priceLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(priceLayout, null);
        }
        if (category.isEmpty()) {
            setLayoutError(categoryLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(categoryLayout, null);
        }
        if (productImageFile == null) {
            productImageText.setVisibility(View.VISIBLE);
            valid = false;
        } else {
            productImageText.setVisibility(View.GONE);
        }

        if(valid){
            checkCodeExists(code, new FirestoreCallback() {
                @Override
                public void onCallback(boolean codeExists) {
                    if (codeExists){
                        setLayoutError(productCodeLayout, "Code Already Exists");
                        progressDialog.dismiss();
                    } else {
                        setLayoutError(productCodeLayout, null);

                        SharedPreferences preferences1 = getSharedPreferences("user", MODE_PRIVATE);
                        String user = preferences1.getString("uid", "");

                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("category")
                                .whereEqualTo("categoryTitle", category)
                                .whereEqualTo("userId", user)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            QuerySnapshot querySnapshot = task.getResult();
                                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                                                String documentId = document.getId();

                                                Map<String, Object> productData = new HashMap<>();
                                                productData.put("userId", user);
                                                productData.put("productName", name);
                                                productData.put("productCode", code);
                                                productData.put("stock", Integer.parseInt(stock));
                                                productData.put("cost", Double.parseDouble(cost));
                                                productData.put("price", Double.parseDouble(price));
                                                productData.put("categoryId", documentId);
                                                productData.put("productStatus", true);
                                                productData.put("profit", profitData);

                                                DocumentReference counterRef = FirebaseFirestore.getInstance().collection("counter").document("product");
                                                counterRef.update("current", FieldValue.increment(1))
                                                        .addOnSuccessListener(aVoid -> {
                                                            counterRef.get().addOnCompleteListener(counterTask -> {
                                                                if (counterTask.isSuccessful()) {
                                                                    DocumentSnapshot counterDocument = counterTask.getResult();
                                                                    if (counterDocument.exists()) {
                                                                        Long newId = counterDocument.getLong("current");

                                                                        db.collection("product")
                                                                                .document(String.valueOf(newId))
                                                                                .set(productData)
                                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                    @Override
                                                                                    public void onSuccess(Void unused) {
                                                                                        uploadToStorage(productImageFile, user, String.valueOf(newId), new OnProductImageUploadedListener(){
                                                                                            @Override
                                                                                            public void onImageUploaded() {
                                                                                                progressDialog.dismiss();
                                                                                                Toast.makeText(getApplicationContext(), "Successful Add Product", Toast.LENGTH_SHORT).show();
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
                                    }
                                });
                    }
                }
            });
        } else {
            progressDialog.dismiss();
        }
    }

    public interface OnProductImageUploadedListener {
        void onImageUploaded();
    }

    private void uploadToStorage(File tempFile, String userID, String productId, OnProductImageUploadedListener listener){
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        Uri fileUri = Uri.fromFile(tempFile);
        StorageReference fileRef = storageRef.child("productImage/" + userID + "/" + productId);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri downloadUrl) {
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                Map<String, Object> imageData = new HashMap<>();
                                imageData.put("productImage", downloadUrl);
                                db.collection("product")
                                        .document(productId)
                                        .set(imageData, SetOptions.merge());
                                listener.onImageUploaded();
                            }
                        });
                    }
                });
    }

    public interface FirestoreCallback {
        void onCallback(boolean codeExists);
    }

    private void checkCodeExists(String code, MPAddProduct.FirestoreCallback callback){
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        SharedPreferences preferences1 = getSharedPreferences("user", MODE_PRIVATE);
        String user = preferences1.getString("uid", "");

        firestore.collection("product")
                .whereEqualTo("productCode", code)
                .whereEqualTo("userId", user).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()){
                        callback.onCallback(true);
                    } else {
                        callback.onCallback(false);
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.mp_add_product);

        backButton = findViewById(R.id.back_button);
        productImage = findViewById(R.id.product_image);
        productImageText = findViewById(R.id.product_image_text);
        categoryLayout = findViewById(R.id.category_layout);
        categoryInput = findViewById(R.id.category_input);
        productNameLayout = findViewById(R.id.product_name_layout);
        productNameInput = findViewById(R.id.product_name_input);
        productCodeLayout = findViewById(R.id.product_code_layout);
        productCodeInput = findViewById(R.id.product_code_input);
        stockLayout = findViewById(R.id.stock_layout);
        stockInput = findViewById(R.id.stock_input);
        costLayout = findViewById(R.id.cost_layout);
        costInput = findViewById(R.id.cost_input);
        priceLayout = findViewById(R.id.price_layout);
        priceInput = findViewById(R.id.price_input);
        profitText = findViewById(R.id.profit_text);
        doneButton = findViewById(R.id.done_button);

        profitData = 0;
        productImageFile = null;

        SharedPreferences preferences1 = getSharedPreferences("user", MODE_PRIVATE);
        String user = preferences1.getString("uid", "");

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        CollectionReference categoryRef = firestore.collection("category");

        categoryRef.whereEqualTo("userId", user)
                .orderBy("categoryTitle", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                            List<String> categoryList = new ArrayList<>();
                            for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                                String categoryTitle = documentSnapshot.getString("categoryTitle");
                                categoryList.add(categoryTitle);
                            }
                            categoryArrayAdapter = new ArrayAdapter<>(this, R.layout.dropdown_menu, categoryList);
                            categoryInput.setAdapter(categoryArrayAdapter);
                        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View focusedView = getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(MPAddProduct.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }
                addNewProduct();
            }
        });

        productImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View focusedView = getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(MPAddProduct.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }
                uploadProductImage();
            }
        });

        categoryInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    View focusedView = getCurrentFocus();
                    if (focusedView != null){
                        focusedView.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(MPAddProduct.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                        }
                    }
                    setLayoutError(categoryLayout, null);
                }
            }
        });

        productNameInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    setLayoutError(productNameLayout, null);
                }
            }
        });

        productCodeInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    setLayoutError(productCodeLayout, null);
                }
            }
        });

        stockInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    setLayoutError(stockLayout, null);
                }
            }
        });

        costInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    setLayoutError(costLayout, null);
                }
            }
        });

        priceInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    setLayoutError(priceLayout, null);
                }
            }
        });

        costInput.addTextChangedListener(new TextWatcher() {
            Locale locale = new Locale("ms", "MY");
            Currency currency = Currency.getInstance(locale);
            int maxDecimals = currency.getDefaultFractionDigits();

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String input = charSequence.toString();

                if (input.contains(".")) {
                    // Find the position of the decimal point
                    int decimalIndex = input.indexOf(".");

                    if (decimalIndex != -1 && input.length() - decimalIndex > maxDecimals + 1) {
                        // If more than allowed decimals are entered, truncate the extra input
                        input = input.substring(0, input.length() - 1);
                        costInput.setText(input);
                        costInput.setSelection(input.length());
                    }
                }

                String priceCurrentInput = priceInput.getText().toString().trim();
                String costCurrentInput = costInput.getText().toString().trim();

                if (!costCurrentInput.isEmpty() && !priceCurrentInput.isEmpty()
                        && !costCurrentInput.equals(".") && !priceCurrentInput.equals(".")){
                    double price = Double.parseDouble(priceCurrentInput);
                    double cost = Double.parseDouble(costCurrentInput);
                    double profit = price - cost;
                    NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
                    String formattedProfit = currencyFormatter.format(profit);
                    BigDecimal roundedProfit = new BigDecimal(profit).setScale(maxDecimals, RoundingMode.HALF_UP);
                    profitData = roundedProfit.doubleValue();
                    profitText.setText("Profit: " + formattedProfit);
                } else {
                    profitData = 0;
                    profitText.setText(R.string.profit_with_value);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        priceInput.addTextChangedListener(new TextWatcher() {
            Locale locale = new Locale("ms", "MY");
            Currency currency = Currency.getInstance(locale);
            int maxDecimals = currency.getDefaultFractionDigits();

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String input = charSequence.toString();
                if (input.contains(".")) {
                    // Find the position of the decimal point
                    int decimalIndex = input.indexOf(".");

                    if (decimalIndex != -1 && input.length() - decimalIndex > maxDecimals + 1) {
                        // If more than allowed decimals are entered, truncate the extra input
                        input = input.substring(0, input.length() - 1);
                        priceInput.setText(input);
                        priceInput.setSelection(input.length());
                    }
                }

                String priceCurrentInput = priceInput.getText().toString().trim();
                String costCurrentInput = costInput.getText().toString().trim();

                if (!costCurrentInput.isEmpty() && !priceCurrentInput.isEmpty()
                        && !costCurrentInput.equals(".") && !priceCurrentInput.equals(".")){
                    double price = Double.parseDouble(priceCurrentInput);
                    double cost = Double.parseDouble(costCurrentInput);
                    double profit = price - cost;
                    NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
                    String formattedProfit = currencyFormatter.format(profit);
                    BigDecimal roundedProfit = new BigDecimal(profit).setScale(maxDecimals, RoundingMode.HALF_UP);
                    profitData = roundedProfit.doubleValue();
                    profitText.setText("Profit: " + formattedProfit);
                } else {
                    profitData = 0;
                    profitText.setText(R.string.profit_with_value);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }
}