package com.example.billsphere;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class StartPage extends AppCompatActivity {

    private void launchUserActivity(String uid) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference userRef = firestore.collection("user").document(uid);
        userRef.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            // Retrieve the userType from the document
                            String userType = documentSnapshot.getString("userType");

                            Intent intent = null;

                            if (userType.equals("customer")) {
                                intent = new Intent(StartPage.this, CustomerActivity.class);
                            } else if (userType.equals("business")) {
                                String businessStatus = documentSnapshot.getString("userBusinessStatus");
                                SharedPreferences preferences = getSharedPreferences("businessStatus", MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString("status", businessStatus);
                                editor.apply();
                                intent = new Intent(StartPage.this, BusinessActivity.class);
                            } else if (userType.equals("admin")) {
                                intent = new Intent(StartPage.this, AdminActivity.class);
                            }

                            if (intent != null) {
                                startActivity(intent);
                                finish();
                            }
                        }
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.start_page);

        // Auto login if the last login user selected remember me
        SharedPreferences preferences = getSharedPreferences("checkbox", MODE_PRIVATE);
        String checkbox = preferences.getString("remember", "");
        SharedPreferences preferences1 = getSharedPreferences("user", MODE_PRIVATE);
        String user = preferences1.getString("uid", "");
        if (checkbox.equals("true") && !user.isEmpty()){
            launchUserActivity(user);
        } else {
            Intent intent = new Intent(StartPage.this, LoginMain.class);
            startActivity(intent);
            finish();
        }
    }
}
