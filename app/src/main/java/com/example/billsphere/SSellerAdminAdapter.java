package com.example.billsphere;

import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class SSellerAdminAdapter extends RecyclerView.Adapter<SSellerAdminAdapter.ViewHolder>{
    private List<SSeller> sellerList;
    private Fragment fragment;

    public SSellerAdminAdapter(List<SSeller> sellerList, Fragment fragment){
        this.sellerList = sellerList;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public SSellerAdminAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SSellerAdminAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.s_seller, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SSellerAdminAdapter.ViewHolder holder, int position) {
        SSeller seller = sellerList.get(position);

        holder.businessName.setText(seller.getBusinessName());
        holder.businessName.setPaintFlags(holder.businessName.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        holder.businessEmail.setText(seller.getBusinessEmail());

        String fullAddress = seller.getBusinessAddress() + ", " + seller.getBusinessPos() + ", " + seller.getBusinessCountry();
        holder.businessFullAddress.setText(fullAddress);

        if (seller.getBusinessImage() != null){
            Glide.with(holder.itemView).load(seller.getBusinessImage())
                    .apply(RequestOptions.circleCropTransform()).into(holder.businessImage);
        } else {
            holder.businessImage.setImageResource(R.drawable.null_profile_image);
        }

        String dialCode = seller.getBusinessDial().split(" ")[0];
        String fullContact = dialCode + " " + seller.getBusinessContact();
        holder.businessFullContact.setText(fullContact);

        holder.itemView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(fragment.getActivity(), SViewDetailAdmin.class);
                intent.putExtra("BUSINESS_ID", seller.getBusinessId());
                fragment.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sellerList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView businessImage;
        TextView businessName, businessEmail, businessFullContact, businessFullAddress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            businessImage = itemView.findViewById(R.id.business_image);
            businessName = itemView.findViewById(R.id.business_name);
            businessEmail = itemView.findViewById(R.id.business_email);
            businessFullContact = itemView.findViewById(R.id.business_phone);
            businessFullAddress = itemView.findViewById(R.id.business_address);
        }
    }
}
