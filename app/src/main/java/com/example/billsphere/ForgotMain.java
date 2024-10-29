package com.example.billsphere;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ForgotMain extends AppCompatActivity {
    private TextInputEditText emailInput, verifyCodeInput;
    private TextInputLayout emailLayout, verifyCodeLayout;
    private Button sendButton, nextButton;
    private TextView loginButton;
    private FirebaseAuth mAuth;

    // Modify the layout
    private void setLayoutError(TextInputLayout layout, String message){
        if (message == null || message.isEmpty()) {
            // No error
            layout.setError(null);
            layout.setErrorEnabled(false);
        } else {
            //With error
            layout.setErrorEnabled(true);
            layout.setError(message);
        }
    }

    // Check the existence of email in firestore database
    private void checkEmailExists(String email, FirestoreCallback callback){
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("user").whereEqualTo("userEmail", email).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()){
                        callback.onCallback(true);
                    } else {
                        callback.onCallback(false);
                    }
                });
    }

    public interface FirestoreCallback {
        void onCallback(boolean emailExists);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.forgot_main);

        emailInput = findViewById(R.id.email_input);
        emailLayout = findViewById(R.id.email_input_layout);
        sendButton = findViewById(R.id.send_button);
        loginButton = findViewById(R.id.login_button);

        mAuth = FirebaseAuth.getInstance();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View focusedView = getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(ForgotMain.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }

                ProgressDialog progressDialog = new ProgressDialog();
                progressDialog.show(getSupportFragmentManager(), "progressDialog");

                String email = emailInput.getText().toString().trim();
                if (email.isEmpty()){
                    setLayoutError(emailLayout, "Required*");
                    progressDialog.dismiss();
                } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    setLayoutError(emailLayout, "Invalid Email Format");
                    progressDialog.dismiss();
                } else {
                    checkEmailExists(email, new FirestoreCallback() {
                        @Override
                        public void onCallback(boolean emailExists) {
                            if (emailExists) {
                                // If email exists, remove the error
                                setLayoutError(emailLayout, null);

                                mAuth.sendPasswordResetEmail(email).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        progressDialog.dismiss();
                                        Toast.makeText(ForgotMain.this, "Check Your Email for Reset Password",
                                                Toast.LENGTH_LONG).show();
                                        Intent loginIntent = new Intent(ForgotMain.this, LoginMain.class);
                                        startActivity(loginIntent);
                                        finish();
                                    }
                                });
                            } else {
                                setLayoutError(emailLayout, "Email does not exist");
                                progressDialog.dismiss();
                            }
                        }
                    });
                }
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent loginIntent = new Intent(ForgotMain.this, LoginMain.class);
                startActivity(loginIntent);
                finish();
            }
        });

        emailInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    setLayoutError(emailLayout, null);
                }
            }
        });
    }
}
