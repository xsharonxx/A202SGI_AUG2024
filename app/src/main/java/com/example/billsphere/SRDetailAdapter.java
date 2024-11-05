package com.example.billsphere;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class SRDetailAdapter extends RecyclerView.Adapter<SRDetailAdapter.ViewHolder>{
    private ArrayList<CBProduct> selectedProducts;
    private Context context;

    public SRDetailAdapter(ArrayList<CBProduct> selectedProducts, Context context){
        this.selectedProducts = selectedProducts;
        this.context = context;
    }

    @NonNull
    @Override
    public SRDetailAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SRDetailAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.sr_detail_card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SRDetailAdapter.ViewHolder holder, int position) {
        CBProduct product = selectedProducts.get(position);

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

        String quantity = String.valueOf(product.getQuantity());
        holder.productQuantity.setText("Quantity: " + quantity);

        double totalAmount = product.getProductPrice() * product.getQuantity();
        String formattedAmount = currencyFormatter.format(totalAmount);
        holder.productTotal.setText("Total: " + formattedAmount);

        double profit = product.getQuantity() * product.getProductProfit();
        String formattedProfit = currencyFormatter.format(profit);
        holder.profit.setText("Profit: " + formattedProfit);
    }

    @Override
    public int getItemCount() {
        return selectedProducts.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView productImage;
        TextView productName, productCategory, productCode, productPrice, productQuantity, productTotal;
        TextView profit;

        public ViewHolder(@NonNull View itemView){
            super(itemView);

            productImage = itemView.findViewById(R.id.product_image);
            productName = itemView.findViewById(R.id.product_name);
            productCode = itemView.findViewById(R.id.product_code);
            productCategory = itemView.findViewById(R.id.category);
            productPrice = itemView.findViewById(R.id.price);
            productQuantity = itemView.findViewById(R.id.quantity);
            productTotal = itemView.findViewById(R.id.total);
            profit = itemView.findViewById(R.id.profit);
        }
    }
}
