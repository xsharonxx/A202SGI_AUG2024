package com.example.billsphere;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class CBCustomerAdapter extends RecyclerView.Adapter<CBCustomerAdapter.ViewHolder> {

    private List<CBCustomer> customerList;
    private Fragment fragment;

    public CBCustomerAdapter(List<CBCustomer> customerList, Fragment fragment){
        this.customerList = customerList;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public CBCustomerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CBCustomerAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.cb_customer, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CBCustomerAdapter.ViewHolder holder, int position) {
        CBCustomer customer = customerList.get(position);

        holder.customerName.setText(customer.getCustomerName());
        holder.customerName.setPaintFlags(holder.customerName.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        holder.customerEmail.setText(customer.getCustomerEmail());

        String fullAddress = customer.getCustomerAddress() + ", " + customer.getCustomerPos() + ", " + customer.getCustomerCountry();
        holder.customerFullAddress.setText(fullAddress);

        if (customer.getCustomerImage() != null){
            Glide.with(holder.itemView).load(customer.getCustomerImage())
                    .apply(RequestOptions.circleCropTransform()).into(holder.customerImage);
        } else {
            holder.customerImage.setImageResource(R.drawable.null_profile_image);
        }

        String dialCode = customer.getCustomerDial().split(" ")[0];
        String fullContact = dialCode + " " + customer.getCustomerContact();
        holder.customerFullContact.setText(fullContact);

        holder.itemView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(fragment.getActivity(), CBAddProduct.class);
                intent.putExtra("CUSTOMER_ID", customer.getCustomerId());
                fragment.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return customerList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView customerImage;
        TextView customerName, customerEmail, customerFullContact, customerFullAddress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            customerImage = itemView.findViewById(R.id.customer_image);
            customerName = itemView.findViewById(R.id.customer_name);
            customerEmail = itemView.findViewById(R.id.customer_email);
            customerFullContact = itemView.findViewById(R.id.customer_phone);
            customerFullAddress = itemView.findViewById(R.id.customer_address);
        }
    }
}
