package com.example.billsphere;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CBPaymentAdapter extends RecyclerView.Adapter<CBPaymentAdapter.ViewHolder>{
    private List<String> paymentMethods;
    private Context context;
    private int selectedPosition;

    public CBPaymentAdapter(List<String> paymentMethods, Context context){
        this.paymentMethods = paymentMethods;
        this.context = context;
        this.selectedPosition = -1;
    }

    @NonNull
    @Override
    public CBPaymentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CBPaymentAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.cb_payment_method, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CBPaymentAdapter.ViewHolder holder, int position) {
        String method = paymentMethods.get(position);

        holder.paymentName.setText(method);

        if (position == selectedPosition) {
            holder.paymentName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.nav_selected));
            holder.tickIcon.setVisibility(View.VISIBLE);
        } else {
            holder.paymentName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.nav_not_selected));
            holder.tickIcon.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int currentPosition = holder.getAdapterPosition();
                selectedPosition = currentPosition;
                notifyDataSetChanged();

                if (context instanceof CBPayment) { // Ensure context is the activity
                    ((CBPayment) context).setSelectedPaymentMethod(paymentMethods.get(selectedPosition));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return paymentMethods.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView paymentName;
        ImageView tickIcon;

        public ViewHolder(@NonNull View itemView){
            super(itemView);

            paymentName = itemView.findViewById(R.id.payment_method);
            tickIcon = itemView.findViewById(R.id.selected_icon);
        }
    }
}
