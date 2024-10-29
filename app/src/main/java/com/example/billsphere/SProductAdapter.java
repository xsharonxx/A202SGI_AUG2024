package com.example.billsphere;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SProductAdapter extends RecyclerView.Adapter<SProductAdapter.ViewHolder>{
    private List<SProduct> productList;
    private Context context;

    public SProductAdapter(List<SProduct> productList, Context context){
        this.productList = productList;
        this.context = context;
    }

    @NonNull
    @Override
    public SProductAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SProductAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.s_product, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SProductAdapter.ViewHolder holder, int position) {
        SProduct product = productList.get(position);

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

        if (!product.isProductStatus()){
            int color = ContextCompat.getColor(context, R.color.error_input);
            holder.productName.setTextColor(color);
            holder.productCode.setTextColor(color);
            holder.productCategory.setTextColor(color);
            holder.productPrice.setTextColor(color);
        } else if (product.isProductStatus()){
            holder.productName.setTextColor(ContextCompat.getColor(context, R.color.nav_selected));
            holder.productPrice.setTextColor(ContextCompat.getColor(context, R.color.nav_selected));
            int color = ContextCompat.getColor(context, R.color.nav_not_selected);
            holder.productCode.setTextColor(color);
            holder.productCategory.setTextColor(color);
        }
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
            productCode = itemView.findViewById(R.id.product_code);
            productCategory = itemView.findViewById(R.id.category);
            productPrice = itemView.findViewById(R.id.price);
        }
    }
}
