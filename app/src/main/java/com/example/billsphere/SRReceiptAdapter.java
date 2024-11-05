package com.example.billsphere;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class SRReceiptAdapter extends RecyclerView.Adapter<SRReceiptAdapter.ViewHolder>{
    private List<SRReceipt> receiptList;
    private Fragment fragment;

    public SRReceiptAdapter(List<SRReceipt> receiptList, Fragment fragment){
        this.receiptList = receiptList;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public SRReceiptAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SRReceiptAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.sr_receipt_card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SRReceiptAdapter.ViewHolder holder, int position) {
        SRReceipt receipt = receiptList.get(position);

        holder.receiptNumber.setText("Receipt " + receipt.getReceiptId());
        holder.receiptNumber.setPaintFlags(holder.receiptNumber.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        String customerId = receipt.getCustomerId();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("user")
                .document(customerId) // Use the customer ID to get the specific document
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String image = document.getString("userImage");
                                String name = document.getString("userName");
                                String address = document.getString("userAddress");
                                String pos = document.getString("userPosCode");
                                String country = document.getString("userCountry");
                                String email = document.getString("userEmail");
                                String dial = document.getString("userDialCode");
                                String contact = document.getString("userContact");

                                if (image != null){
                                    Glide.with(fragment).load(image)
                                            .apply(RequestOptions.circleCropTransform()).into(holder.customerImage);
                                } else {
                                    holder.customerImage.setImageResource(R.drawable.null_profile_image);
                                }
                                holder.customerName.setText(name);
                                holder.customerEmail.setText(email);
                                String fullAddress = address + ", " + pos + ", " + country;
                                holder.customerAddress.setText(fullAddress);
                                String dialCode = dial.split(" ")[0];
                                String fullContact = dialCode + " " + contact;
                                holder.customerPhone.setText(fullContact);
                            }
                        }
                    }
                });

        LocalDateTime fullDateTime = receipt.getTimestamp();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String date = fullDateTime.format(dateFormatter);
        String time = fullDateTime.format(timeFormatter);
        holder.billDate.setText(date);
        holder.billTime.setText(time);
        holder.billMethod.setText("Paid by " + receipt.getPaymentMethod());

        Locale locale = new Locale("ms", "MY");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
        String formattedTotal = currencyFormatter.format(receipt.getTotalAmount());
        holder.totalAmount.setText(formattedTotal);
        double profit = 0;
        for (CBProduct product : receipt.getSelectedProductList()){
            profit = profit + (product.getQuantity() * product.getProductProfit());
        }
        String formattedProfit = currencyFormatter.format(profit);
        holder.profitAmount.setText("Profit: " + formattedProfit);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(fragment.getActivity(), SRBillDetail.class);
                intent.putExtra("RECEIPT_ID", receipt.getReceiptId());
                fragment.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return receiptList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView customerImage;
        private TextView billDate, billTime, billMethod;
        private TextView receiptNumber, totalAmount, profitAmount;
        private TextView customerName, customerEmail, customerPhone, customerAddress;

        public ViewHolder(@NonNull View itemView){
            super(itemView);

            customerImage = itemView.findViewById(R.id.customer_image);
            billDate = itemView.findViewById(R.id.bill_date);
            billTime = itemView.findViewById(R.id.bill_time);
            billMethod = itemView.findViewById(R.id.bill_method);
            receiptNumber = itemView.findViewById(R.id.receipt_number);
            totalAmount = itemView.findViewById(R.id.total_amount);
            profitAmount = itemView.findViewById(R.id.profit_amount);
            customerName = itemView.findViewById(R.id.customer_name);
            customerEmail = itemView.findViewById(R.id.customer_email);
            customerPhone = itemView.findViewById(R.id.customer_phone);
            customerAddress = itemView.findViewById(R.id.customer_address);

        }
    }
}
