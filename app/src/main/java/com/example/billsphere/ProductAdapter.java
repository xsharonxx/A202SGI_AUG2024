package com.example.billsphere;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private List<Product> productList;
    private Fragment fragment;

    public ProductAdapter(List<Product> productList, Fragment fragment){
        this.productList = productList;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.mp_product, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.productName.setText(product.getProductName());
        holder.productCode.setText(product.getProductCode());
        Glide.with(holder.itemView).load(product.getProductImage())
                .apply(RequestOptions.centerCropTransform()).into(holder.productImage);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("category").document(product.getCategoryId());
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    Map<String, Object> categoryData = documentSnapshot.getData();

                    if (categoryData != null) {
                        String categoryName = (String) categoryData.get("categoryTitle");
                        holder.category.setText(categoryName);
                    }
                }
            }
        });

        String stockString = String.valueOf(product.getStock());
        holder.stock.setText("Stock: " + stockString);

        Locale locale = new Locale("ms", "MY");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
        String formattedCost = currencyFormatter.format(product.getCost());
        holder.cost.setText("Cost: " + formattedCost);
        String formattedPrice = currencyFormatter.format(product.getPrice());
        holder.price.setText(formattedPrice);
        String formattedProfit = currencyFormatter.format(product.getProfit());
        holder.profit.setText("Profit: " + formattedProfit);

        holder.editProductButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(fragment.getActivity(), MPEditProduct.class);
                intent.putExtra("PRODUCT_ID", product.getProductId());
                fragment.startActivity(intent);
            }
        });

        if (!product.isProductStatus()) {
            int color = ContextCompat.getColor(fragment.requireContext(), R.color.error_input);
            holder.productName.setTextColor(color);
            holder.productCode.setTextColor(color);
            holder.category.setTextColor(color);
            holder.price.setTextColor(color);
            holder.stock.setTextColor(color);
            holder.cost.setTextColor(color);
            holder.profit.setTextColor(color);
        } else if (product.isProductStatus()){
            holder.productName.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.nav_selected));
            holder.price.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.nav_selected));
            int color = ContextCompat.getColor(fragment.requireContext(), R.color.nav_not_selected);
            holder.productCode.setTextColor(color);
            holder.category.setTextColor(color);
            holder.stock.setTextColor(color);
            holder.cost.setTextColor(color);
            holder.profit.setTextColor(color);
        }
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView productImage;
        TextView productName, productCode, category, price, stock, cost, profit;
        ImageButton editProductButton;

        public ViewHolder(@NonNull View itemView){
            super(itemView);

            productImage = itemView.findViewById(R.id.product_image);
            productName = itemView.findViewById(R.id.product_name);
            productCode = itemView.findViewById(R.id.product_code);
            category = itemView.findViewById(R.id.category);
            price = itemView.findViewById(R.id.price);
            stock = itemView.findViewById(R.id.stock);
            cost = itemView.findViewById(R.id.cost);
            profit = itemView.findViewById(R.id.profit);
            editProductButton = itemView.findViewById(R.id.edit_product_button);

        }
    }
}
