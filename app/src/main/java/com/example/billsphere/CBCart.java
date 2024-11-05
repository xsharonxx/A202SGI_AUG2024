package com.example.billsphere;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CBCart extends AppCompatActivity implements CBCartProductAdapter.OnQuantityUpdateListener{

    private ImageButton backButton;
    private ImageView customerImage;
    private TextView customerName, customerEmail, customerPhone, customerAddress;
    private RecyclerView recyclerView;
    private TextView totalText;
    private Button payButton;

    private String customerId;

    private CBCartProductAdapter productAdapter;

    private void updateTotalAmount(){
        double totalAmount = 0;
        for (CBProduct product : ProductCartManager.getInstance().getSelectedProducts()){
            totalAmount = totalAmount + (product.getQuantity() * product.getProductPrice());
        }
        Locale locale = new Locale("ms", "MY");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
        String formattedTotal = currencyFormatter.format(totalAmount);
        totalText.setText("Total: " + formattedTotal);
    }

    @Override
    public void onQuantityUpdated() {
        updateTotalAmount();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.cb_cart);

        Intent intent = getIntent();
        customerId = intent.getStringExtra("CUSTOMER_ID");

        backButton = findViewById(R.id.back_button);
        customerImage = findViewById(R.id.customer_image);
        customerName = findViewById(R.id.customer_name);
        customerEmail = findViewById(R.id.customer_email);
        customerPhone = findViewById(R.id.customer_phone);
        customerAddress = findViewById(R.id.customer_address);
        recyclerView = findViewById(R.id.recycle_view);
        totalText = findViewById(R.id.total_text);
        payButton = findViewById(R.id.pay_button);

        recyclerView.setLayoutManager(new LinearLayoutManager(CBCart.this));
        productAdapter = new CBCartProductAdapter(ProductCartManager.getInstance().getSelectedProducts(), CBCart.this, this);
        recyclerView.setAdapter(productAdapter);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("user") // Replace "users" with your actual collection name
                .document(customerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                           @Override
                                           public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                               if (task.isSuccessful()) {
                                                   DocumentSnapshot document = task.getResult();
                                                   if (document != null && document.exists()) {
                                                       String image = document.getString("userImage");
                                                       String name = document.getString("userName");
                                                       String address = document.getString("userAddress");
                                                       String pos = document.getString("userPosCode");
                                                       String country = document.getString("userCountry");
                                                       String email = document.getString("userEmail");
                                                       String dial = document.getString("userDialCode");
                                                       String contact = document.getString("userContact");

                                                       customerName.setText(name);
                                                       customerName.setPaintFlags(customerName.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

                                                       customerEmail.setText(email);

                                                       String fullAddress = address + ", " + pos + ", " + country;
                                                       customerAddress.setText(fullAddress);

                                                       if (image != null){
                                                           Glide.with(CBCart.this).load(image)
                                                                   .apply(RequestOptions.circleCropTransform()).into(customerImage);
                                                       } else {
                                                           customerImage.setImageResource(R.drawable.null_profile_image);
                                                       }

                                                       String dialCode = dial.split(" ")[0];
                                                       String fullContact = dialCode + " " + contact;
                                                       customerPhone.setText(fullContact);
                                                   }
                                               }
                                           }
                                       });

        updateTotalAmount();

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        payButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CBCart.this, CBPayment.class);
                intent.putExtra("CUSTOMER_ID", customerId);
                String stringTotal = totalText.getText().toString().trim();
                String numericTotal = stringTotal.replaceAll("Total: ", "").trim();
                intent.putExtra("TOTAL_AMOUNT", numericTotal);
                startActivity(intent);
            }
        });
    }
}
