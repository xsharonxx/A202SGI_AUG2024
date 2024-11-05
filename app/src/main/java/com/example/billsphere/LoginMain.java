package com.example.billsphere;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginMain extends AppCompatActivity {

    private TextInputEditText emailInput, passwordInput;
    private CheckBox rememberMeCheckbox;
    private Button loginButton;
    private TextView forgotPasswordButton, signUpButton;
    private TextInputLayout emailLayout, passwordLayout;
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
        firestore.collection("user")
                .whereEqualTo("userEmail", email)
                .get()
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
                                intent = new Intent(LoginMain.this, CustomerActivity.class);
                            } else if (userType.equals("business")) {
                                String businessStatus = documentSnapshot.getString("userBusinessStatus");
                                SharedPreferences preferences = getSharedPreferences("businessStatus", MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString("status", businessStatus);
                                editor.apply();
                                intent = new Intent(LoginMain.this, BusinessActivity.class);
                            } else if (userType.equals("admin")) {
                                intent = new Intent(LoginMain.this, AdminActivity.class);
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
        setContentView(R.layout.login_main);

        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        rememberMeCheckbox = findViewById(R.id.remember_me_checkbox);
        loginButton = findViewById(R.id.login_button);
        forgotPasswordButton = findViewById(R.id.forgot_password_button);
        signUpButton = findViewById(R.id.sign_up_button);
        emailLayout = findViewById(R.id.email_input_layout);
        passwordLayout = findViewById(R.id.password_input_layout);

        mAuth = FirebaseAuth.getInstance();

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View focusedView = getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(LoginMain.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }

                ProgressDialog progressDialog = new ProgressDialog();
                progressDialog.show(getSupportFragmentManager(), "progressDialog");

                String email = emailInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();

                // Ensure email and password are entered
                if (email.isEmpty() && password.isEmpty()){
                    // If both email and password are blank
                    setLayoutError(emailLayout, "Required*");
                    setLayoutError(passwordLayout, "Required*");
                    progressDialog.dismiss();
                    return;
                } else {
                    if (email.isEmpty()){
                        // If email is blank
                        setLayoutError(emailLayout, "Required*");
                        progressDialog.dismiss();
                        return;
                    } else {
                        setLayoutError(emailLayout, null);
                    }
                    if (password.isEmpty()){
                        // If password is blank
                        setLayoutError(passwordLayout, "Required*");
                        progressDialog.dismiss();
                        return;
                    } else {
                        setLayoutError(passwordLayout, null);
                    }
                }

                // Verify the email format
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    setLayoutError(emailLayout, "Invalid Email Format");
                    progressDialog.dismiss();
                    return;
                }
                else {
                    setLayoutError(emailLayout, null);
                }

                checkEmailExists(email, new FirestoreCallback() {
                    @Override
                    public void onCallback(boolean emailExists) {
                        if (emailExists) {
                            // If email exists, continue with login
                            setLayoutError(emailLayout, null);
                            mAuth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            if (task.isSuccessful()) {
                                                // Login credential correct
                                                FirebaseUser user = mAuth.getCurrentUser();

                                                String uid = user.getUid();
                                                SharedPreferences preferences = getSharedPreferences("user", MODE_PRIVATE);
                                                SharedPreferences.Editor editor = preferences.edit();
                                                editor.putString("uid", uid);
                                                editor.apply();

                                                if (rememberMeCheckbox.isChecked()){
                                                    // User ticked remember me
                                                    SharedPreferences preferences1 = getSharedPreferences("checkbox", MODE_PRIVATE);
                                                    SharedPreferences.Editor editor1 = preferences1.edit();
                                                    editor1.putString("remember", "true");
                                                    editor1.apply();
                                                }

                                                progressDialog.dismiss();
                                                Toast.makeText(LoginMain.this, "Successful Login.",
                                                        Toast.LENGTH_SHORT).show();
                                                launchUserActivity(uid);
                                            } else {
                                                // Incorrect login credential (typically password)
                                                if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                                    setLayoutError(passwordLayout, "Incorrect password");
                                                    progressDialog.dismiss();
                                                } else {
                                                    progressDialog.dismiss();
                                                    Toast.makeText(LoginMain.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        }
                                    });
                        } else {
                            setLayoutError(emailLayout, "Email does not exist");
                            progressDialog.dismiss();
                        }
                    }
                });
            }
        });

        forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent forgotPasswordIntent = new Intent(LoginMain.this, ForgotMain.class);
                startActivity(forgotPasswordIntent);
                finish();
            }
        });

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signUpIntent = new Intent(LoginMain.this, RegisterMain.class);
                startActivity(signUpIntent);
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

        passwordInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    setLayoutError(passwordLayout, null);
                }
            }
        });
    }
}
