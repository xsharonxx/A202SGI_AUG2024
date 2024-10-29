package com.example.billsphere;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class ProfileAdmin extends AppCompatActivity {

    private ImageButton backButton;
    private TextView userEmail;
    private ImageView userImage;
    private TextInputEditText nameInput;
    private TextInputLayout nameLayout;
    private TextView changePasswordButton;
    private Button saveButton;

    private File userImageFile;

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

    private void uploadUserImage(){
        ImagePicker.with(this).cropSquare().compress(512).maxResultSize(512, 512)
                .createIntent(new Function1<Intent, Unit>() {
                    @Override
                    public Unit invoke(Intent intent) {
                        pickUserImageLauncher.launch(intent);
                        return null;
                    }
                });
    }

    private final ActivityResultLauncher<Intent> pickUserImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        Glide.with(this).load(fileUri).apply(RequestOptions.circleCropTransform()).into(userImage);
                        userImageFile = saveFileToTempStorage(fileUri);
                    }
                }
            });

    private File saveFileToTempStorage(Uri fileUri) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileUri.toString());
        File tempFile = new File(getCacheDir(), "tempFile." + extension);

        try (InputStream inputStream = getContentResolver().openInputStream(fileUri);
             OutputStream outputStream = new FileOutputStream(tempFile)) {
            if (inputStream != null) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return tempFile;
    }

    private void sendChangePassword(){
        ProgressDialog progressDialog = new ProgressDialog();
        progressDialog.show(getSupportFragmentManager(), "progressDialog");
        String email = userEmail.getText().toString().trim();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.sendPasswordResetEmail(email).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                progressDialog.dismiss();
                Toast.makeText(ProfileAdmin.this, "Check Your Email for Reset Password",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    public interface OnUserImageUploadedListener {
        void onImageUploaded();
    }

    private void uploadToStorage(File tempFile, String userID, OnUserImageUploadedListener listener){
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        Uri fileUri = Uri.fromFile(tempFile);
        StorageReference fileRef = storageRef.child("userImage/" + userID);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri downloadUrl) {
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                Map<String, Object> imageData = new HashMap<>();
                                imageData.put("userImage", downloadUrl);
                                db.collection("user")
                                        .document(userID)
                                        .set(imageData, SetOptions.merge());
                                listener.onImageUploaded();
                            }
                        });
                    }
                });
    }

    private void validateSave(){
        ProgressDialog progressDialog = new ProgressDialog();
        progressDialog.show(getSupportFragmentManager(), "progressDialog");
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            setLayoutError(nameLayout, "Required*");
            progressDialog.dismiss();
        } else {
            setLayoutError(nameLayout, null);

            Map<String, Object> userData = new HashMap<>();
            userData.put("userName", name);

            SharedPreferences preferences1 = getSharedPreferences("user", MODE_PRIVATE);
            String user = preferences1.getString("uid", "");

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("user")
                    .document(user)
                    .update(userData)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            if (userImageFile != null) {
                                uploadToStorage(userImageFile, user, new ProfileAdmin.OnUserImageUploadedListener() {
                                    @Override
                                    public void onImageUploaded() {
                                        progressDialog.dismiss();
                                        Toast.makeText(getApplicationContext(), "Successful Edit", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                });
                            } else {
                                progressDialog.dismiss();
                                Toast.makeText(getApplicationContext(), "Successful Edit", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }
                    });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.profile_admin);

        backButton = findViewById(R.id.back_button);
        userEmail = findViewById(R.id.email);
        userImage = findViewById(R.id.image);
        nameInput = findViewById(R.id.name_input);
        nameLayout = findViewById(R.id.name_layout);
        changePasswordButton = findViewById(R.id.change_password_button);
        saveButton = findViewById(R.id.save_button);

        userImageFile = null;

        SharedPreferences preferences1 = getSharedPreferences("user", MODE_PRIVATE);
        String user = preferences1.getString("uid", "");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("user").document(user);
        userRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    String name = documentSnapshot.getString("userName");
                    String email = documentSnapshot.getString("userEmail");
                    String image = documentSnapshot.getString("userImage");

                    userEmail.setText(email);
                    nameInput.setText(name);
                    if (image != null){
                        Glide.with(ProfileAdmin.this).load(image)
                                .apply(RequestOptions.circleCropTransform()).into(userImage);
                    } else {
                        userImage.setImageResource(R.drawable.null_profile_image);
                    }
                }
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View focusedView = getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(ProfileAdmin.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }
                validateSave();
            }
        });

        userImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View focusedView = getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(ProfileAdmin.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }
                uploadUserImage();
            }
        });

        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View focusedView = getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(ProfileCustomer.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }
                sendChangePassword();
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        nameInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    setLayoutError(nameLayout, null);
                }
            }
        });
    }
}
