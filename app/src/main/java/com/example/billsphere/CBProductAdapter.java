package com.example.billsphere;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CBProductAdapter extends RecyclerView.Adapter<CBProductAdapter.ViewHolder>{
    private List<CBProduct> productList;
    private Context context;
    private ArrayList<CBProduct> selectedProducts;
    private OnSelectedProductUpdateListener listener;

    public interface OnSelectedProductUpdateListener {
        void onSelectedProductUpdated();
    }

    public CBProductAdapter(List<CBProduct> productList, Context context, ArrayList<CBProduct> selectedProducts,
                            OnSelectedProductUpdateListener listener){
        this.productList = productList;
        this.context = context;
        this.selectedProducts = selectedProducts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CBProductAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CBProductAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.cb_product_card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CBProductAdapter.ViewHolder holder, int position) {
        CBProduct product = productList.get(position);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("category").document(product.getCategoryId());
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    Map<String, Object> categoryData = documentSnapshot.getData();

                    if (categoryData != null) {
                        String categoryName = (String) categoryData.get("categoryTitle");
                        holder.productCategory.setText(categoryName);
                    }
                }
            }
        });

        Glide.with(holder.itemView).load(product.getProductImage())
                .apply(RequestOptions.centerCropTransform()).into(holder.productImage);
        holder.productName.setText(product.getProductName());
        holder.productCode.setText(product.getProductCode());

        Locale locale = new Locale("ms", "MY");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
        String formattedPrice = currencyFormatter.format(product.getProductPrice());
        holder.productPrice.setText(formattedPrice);

        holder.itemView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                View focusedView = ((Activity) context).getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }

                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
                View bottomSheetView = ((Activity) context).getLayoutInflater().inflate(R.layout.cb_bottom_sheet, null);
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

                productName.setText(holder.productName.getText());
                productCategory.setText(holder.productCategory.getText());
                productCode.setText(holder.productCode.getText());
                productPrice.setText(holder.productPrice.getText());

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

                            for (CBProduct selectedProduct : selectedProducts) {
                                if (selectedProduct.getProductCode().equals(product.getProductCode())) {
                                    selectedProduct.setQuantity(selectedProduct.getQuantity() + quantityInt);
                                    productExists = true;
                                    break;
                                }
                            }

                            if (!productExists){
                                product.setQuantity(quantityInt);
                                selectedProducts.add(product);
                            }

                            listener.onSelectedProductUpdated();
                            bottomSheetDialog.dismiss();
                        } else {
                            quantityField.setError("Required*");
                        }
                    }
                });

                bottomSheetDialog.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, productCategory, productCode, productPrice;

        public ViewHolder(@NonNull View itemView){
            super(itemView);

            productImage = itemView.findViewById(R.id.product_image);
            productName = itemView.findViewById(R.id.product_name);
            productCategory = itemView.findViewById(R.id.category);
            productCode = itemView.findViewById(R.id.product_code);
            productPrice = itemView.findViewById(R.id.price);
        }
    }
}
