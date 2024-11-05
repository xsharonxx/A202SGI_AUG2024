package com.example.billsphere;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Date;
import java.text.ParseException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.UploadTask;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONObject;

public class RegisterMain extends AppCompatActivity {

    private TextInputLayout accountTypeLayout, dialCodeLayout, countryLayout;
    private AutoCompleteTextView accountTypeAutoComplete, dialCodeAutoComplete, countryAutoComplete;
    private TextInputLayout contactLayout, posCodeLayout, passwordLayout, confirmPasswordLayout, emailLayout;
    private TextInputEditText contactInput, posCodeInput, passwordInput, confirmPasswordInput, emailInput;
    // Business Specific Info
    private TextInputLayout businessNameLayout, businessAddressLayout;
    private TextInputEditText businessNameInput, businessAddressInput;
    private TextInputLayout businessProofLayout, ownerProofLayout;
    private TextInputEditText businessProofInput, ownerProofInput;
    // Customer Specific Info
    private TextInputLayout nameLayout, addressLayout, dobLayout;
    private TextInputEditText nameInput, addressInput, dobInput;
    private TextInputLayout genderLayout;
    private AutoCompleteTextView genderAutoComplete;
    //Button
    private Button doneButton;
    private TextView loginButton;

    private TextView agreementText;

    private ArrayAdapter<String> accountTypeAdapter, dialCodeAdapter, countryAdapter, genderAdapter;

    private File businessDocument, ownerDocument;

    private FirebaseAuth mAuth;

    private void clearField(){
        dialCodeAutoComplete.setText("");
        countryAutoComplete.setText("");
        contactInput.setText("");
        posCodeInput.setText("");
        passwordInput.setText("");
        confirmPasswordInput.setText("");
        emailInput.setText("");
        businessNameInput.setText("");
        businessAddressInput.setText("");
        businessProofInput.setText("");
        ownerProofInput.setText("");
        nameInput.setText("");
        addressInput.setText("");
        dobInput.setText("");
        genderAutoComplete.setText("");

        passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
        confirmPasswordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());

        businessNameLayout.setVisibility(View.GONE);
        businessAddressLayout.setVisibility(View.GONE);
        businessProofLayout.setVisibility(View.GONE);
        ownerProofLayout.setVisibility(View.GONE);
        nameLayout.setVisibility(View.GONE);
        addressLayout.setVisibility(View.GONE);
        dobLayout.setVisibility(View.GONE);
        genderLayout.setVisibility(View.GONE);

        businessDocument = null;
        ownerDocument = null;

        setLayoutError(dialCodeLayout, null);
        setLayoutError(countryLayout, null);
        setLayoutError(contactLayout, null);
        setLayoutError(posCodeLayout, null);
        setLayoutError(passwordLayout, null);
        setLayoutError(confirmPasswordLayout, null);
        setLayoutError(emailLayout, null);
        setLayoutError(businessNameLayout, null);
        setLayoutError(businessAddressLayout, null);
        setLayoutError(businessProofLayout, null);
        setLayoutError(ownerProofLayout, null);
        setLayoutError(nameLayout, null);
        setLayoutError(addressLayout, null);
        setLayoutError(dobLayout, null);
        setLayoutError(genderLayout, null);
    }

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

    private String getFileName(Uri fileUri){
        String filename = null;
        String[] projection = { MediaStore.Images.Media.DISPLAY_NAME };
        try (Cursor cursor = getContentResolver().query(fileUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                filename = cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filename;
    }

    private File saveFileToTempStorage(Uri fileUri) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileUri.toString());
        String uniqueFileName = "tempFile_" + System.currentTimeMillis() + "." + extension;
        File tempFile = new File(getCacheDir(), "tempFile." + uniqueFileName);

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

    private final ActivityResultLauncher<Intent> pickBusinessPdfLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        String filename = getFileName(fileUri);
                        if (filename != null) {
                            businessProofInput.setText(filename);
                        }
                        File tempFile = saveFileToTempStorage(fileUri);
                        if (tempFile != null) {
                            businessDocument = tempFile;
                        }
                        setLayoutError(businessProofLayout, null);
                    }
                }
            });

    private void uploadBusinessDocument(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        pickBusinessPdfLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> pickOwnerPdfLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        String filename = getFileName(fileUri);
                        if (filename != null) {
                            ownerProofInput.setText(filename);
                        }
                        File tempFile = saveFileToTempStorage(fileUri);
                        if (tempFile != null) {
                            ownerDocument = tempFile;
                        }
                        setLayoutError(ownerProofLayout, null);
                    }
                }
            });

    private void uploadCustomerDocument(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        pickOwnerPdfLauncher.launch(intent);
    }

    // Check the existence of email in firestore database
    private void checkEmailExists(String email, RegisterMain.FirestoreCallback callback){
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

    public interface VerificationCallback {
        void onVerificationResult(boolean isValid);
    }

    private void verifyEmailPassword(String email, String password, String confirmPassword, VerificationCallback callback){
        // Verify the email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            setLayoutError(emailLayout, "Invalid Email Format");
            callback.onVerificationResult(false);
            return;
        }
        else {
            setLayoutError(emailLayout, null);
            checkEmailExists(email, new RegisterMain.FirestoreCallback() {
                @Override
                public void onCallback(boolean emailExists) {
                    if (emailExists) {
                        setLayoutError(emailLayout, "Email Already Exists");
                        callback.onVerificationResult(false);
                        return;
                    } else {
                        setLayoutError(emailLayout, null);
                        if (password.length() < 8) {
                            setLayoutError(passwordLayout, "At least 8 characters");
                            callback.onVerificationResult(false);
                            return;
                        } else {
                            setLayoutError(passwordLayout, null);
                            if (!password.equals(confirmPassword)) {
                                setLayoutError(confirmPasswordLayout, "Passwords do not match");
                                callback.onVerificationResult(false);
                                return;
                            } else {
                                setLayoutError(confirmPasswordLayout, null);
                                callback.onVerificationResult(true);
                            }
                        }
                    }
                }
            });
        }
    }

    private boolean verifyDialCodeContact(String dialCode, String contact){
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            String country = dialCode.split("\\(")[1].replaceAll("\\)", "").trim();
            String isoCountry = null;
            for (String isoCountryCode : Locale.getISOCountries()) {
                Locale locale = new Locale("", isoCountryCode);
                String displayCountry = locale.getDisplayCountry(Locale.ENGLISH);

                if (displayCountry.equalsIgnoreCase(country)) {
                    isoCountry = isoCountryCode.toUpperCase();
                }
            }
            PhoneNumber number = phoneNumberUtil.parse(contact, isoCountry);
            return phoneNumberUtil.isValidNumber(number);
        } catch (NumberParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean verifyCountryPosCode(String posCode, String country) {
        try {
            String urlString = "https://nominatim.openstreetmap.org/search?format=json&q=" +
                    URLEncoder.encode(posCode + " " + country, "UTF-8");
            Log.d("API Request", "URL String: " + urlString);

            // Create URL object
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            // Get response code
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Parse JSON response
                JSONArray jsonResponse = new JSONArray(response.toString());
                if (jsonResponse.length() > 0) {
                    JSONObject firstResult = jsonResponse.getJSONObject(0);

                    String responsePostcode = firstResult.optString("name", "");  // Get postcode, use fallback if missing
                    String displayName = firstResult.optString("display_name", "");  // Get the full address, use fallback if missing
                    String addressType = firstResult.optString("addresstype", "");  // Get address type, use fallback if missing

                    // Refined check, ensure null or empty checks
                    if (!responsePostcode.isEmpty() && !displayName.isEmpty() && "postcode".equalsIgnoreCase(addressType) && displayName.contains(country) && responsePostcode.equals(posCode)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void customerRegister(){
        ProgressDialog progressDialog = new ProgressDialog();
        progressDialog.show(getSupportFragmentManager(), "progressDialog");

        String name = nameInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String dob = dobInput.getText().toString().trim();
        String gender = genderAutoComplete.getText().toString().trim();
        String contact = contactInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        String posCode = posCodeInput.getText().toString().trim();
        String dialCode = dialCodeAutoComplete.getText().toString().trim();
        String country = countryAutoComplete.getText().toString().trim();

        boolean valid = true;

        if (name.isEmpty()) {
            setLayoutError(nameLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(nameLayout, null);
        }
        if (address.isEmpty()) {
            setLayoutError(addressLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(addressLayout, null);
        }
        if (dob.isEmpty()) {
            setLayoutError(dobLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(dobLayout, null);
        }
        if (gender.isEmpty()) {
            setLayoutError(genderLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(genderLayout, null);
        }
        if (contact.isEmpty()) {
            setLayoutError(contactLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(contactLayout, null);
        }
        if (email.isEmpty()) {
            setLayoutError(emailLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(emailLayout, null);
        }
        if (password.isEmpty()) {
            setLayoutError(passwordLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(passwordLayout, null);
        }
        if (confirmPassword.isEmpty()) {
            setLayoutError(confirmPasswordLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(confirmPasswordLayout, null);
        }
        if (dialCode.isEmpty()) {
            setLayoutError(dialCodeLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(dialCodeLayout, null);
        }
        if (country.isEmpty()) {
            setLayoutError(countryLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(countryLayout, null);
        }
        if (posCode.isEmpty()) {
            setLayoutError(posCodeLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(posCodeLayout, null);
        }

        if (valid){
            boolean[] validationResults = new boolean[3];

            boolean dialCodeContact = verifyDialCodeContact(dialCode, contact);
            validationResults[0] = dialCodeContact;
            if (dialCodeContact){
                setLayoutError(contactLayout, null);
            } else {
                setLayoutError(contactLayout, "Invalid Phone Format");
            }

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executor.execute(() -> {
                // Perform the network request here
                boolean countryPosCode = verifyCountryPosCode(posCode, country);  // Your existing verification method
                validationResults[1] = countryPosCode;

                handler.post(() -> {
                    // This runs on the UI thread, update the UI based on the result
                    if (countryPosCode) {
                        setLayoutError(posCodeLayout, null);
                    } else {
                        setLayoutError(posCodeLayout, "Invalid Pos Code");
                    }

                    verifyEmailPassword(email, password, confirmPassword, new VerificationCallback() {
                        @Override
                        public void onVerificationResult(boolean isValid) {
                            if (isValid){
                                validationResults[2] = true;
                            } else {
                                validationResults[2] = false;
                            }

                            boolean allValid = true;
                            for (boolean result : validationResults){
                                if(!result){
                                    allValid = false;
                                    break;
                                }
                            }

                            if (allValid){
                                mAuth.createUserWithEmailAndPassword(email, password)
                                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                            @Override
                                            public void onComplete(@NonNull Task<AuthResult> task) {
                                                if (task.isSuccessful()) {
                                                    FirebaseUser user = mAuth.getCurrentUser();
                                                    if (user != null){
                                                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                                                        try {
                                                            Date dateOfBirth = sdf.parse(dob);
                                                            Timestamp dobTimestamp = new Timestamp(dateOfBirth);

                                                            Map<String, Object> userData = new HashMap<>();
                                                            userData.put("userType", "customer");
                                                            userData.put("userEmail", email);
                                                            userData.put("userName", name);
                                                            userData.put("userAddress", address);
                                                            userData.put("userDOB", dobTimestamp);
                                                            userData.put("userGender", gender);
                                                            userData.put("userContact", contact);
                                                            userData.put("userPosCode", posCode);
                                                            userData.put("userDialCode", dialCode);
                                                            userData.put("userCountry", country);

                                                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                                                            db.collection("user")
                                                                    .document(user.getUid())
                                                                    .set(userData)
                                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                        @Override
                                                                        public void onSuccess(Void unused) {
                                                                            progressDialog.dismiss();
                                                                            Toast.makeText(getApplicationContext(), "Successful Sign Up", Toast.LENGTH_SHORT).show();
                                                                            Intent loginIntent = new Intent(RegisterMain.this, LoginMain.class);
                                                                            startActivity(loginIntent);
                                                                            finish();
                                                                        }
                                                                    });
                                                        } catch (ParseException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                } else {
                                                    progressDialog.dismiss();
                                                    Toast.makeText(RegisterMain.this, "Failed to sign up", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            } else {
                                progressDialog.dismiss();
                            }
                        }
                    });
                });
            });
        } else {
            progressDialog.dismiss();
        }
    }

    private void businessRegister(){
        ProgressDialog progressDialog = new ProgressDialog();
        progressDialog.show(getSupportFragmentManager(), "progressDialog");

        String businessName = businessNameInput.getText().toString().trim();
        String businessAddress = businessAddressInput.getText().toString().trim();
        String businessProof = businessProofInput.getText().toString().trim();
        String ownerProof = ownerProofInput.getText().toString().trim();
        String contact = contactInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        String posCode = posCodeInput.getText().toString().trim();
        String dialCode = dialCodeAutoComplete.getText().toString().trim();
        String country = countryAutoComplete.getText().toString().trim();

        boolean valid = true;

        if (businessName.isEmpty()) {
            setLayoutError(businessNameLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(businessNameLayout, null);
        }
        if (businessAddress.isEmpty()) {
            setLayoutError(businessAddressLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(businessAddressLayout, null);
        }
        if (businessProof.isEmpty()) {
            setLayoutError(businessProofLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(businessProofLayout, null);
        }
        if (ownerProof.isEmpty()) {
            setLayoutError(ownerProofLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(ownerProofLayout, null);
        }
        if (contact.isEmpty()) {
            setLayoutError(contactLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(contactLayout, null);
        }
        if (email.isEmpty()) {
            setLayoutError(emailLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(emailLayout, null);
        }
        if (password.isEmpty()) {
            setLayoutError(passwordLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(passwordLayout, null);
        }
        if (confirmPassword.isEmpty()) {
            setLayoutError(confirmPasswordLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(confirmPasswordLayout, null);
        }
        if (dialCode.isEmpty()) {
            setLayoutError(dialCodeLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(dialCodeLayout, null);
        }
        if (country.isEmpty()) {
            setLayoutError(countryLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(countryLayout, null);
        }
        if (posCode.isEmpty()) {
            setLayoutError(posCodeLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(posCodeLayout, null);
        }

        if (valid){
            boolean[] validationResults = new boolean[3];

            boolean dialCodeContact = verifyDialCodeContact(dialCode, contact);
            validationResults[0] = dialCodeContact;
            if (dialCodeContact){
                setLayoutError(contactLayout, null);
            } else {
                setLayoutError(contactLayout, "Invalid Phone Format");
            }

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executor.execute(() -> {
                // Perform the network request here
                boolean countryPosCode = verifyCountryPosCode(posCode, country);  // Your existing verification method
                validationResults[1] = countryPosCode;

                handler.post(() -> {
                    // This runs on the UI thread, update the UI based on the result
                    if (countryPosCode) {
                        setLayoutError(posCodeLayout, null);
                    } else {
                        setLayoutError(posCodeLayout, "Invalid Pos Code");
                    }

                    verifyEmailPassword(email, password, confirmPassword, new VerificationCallback() {
                        @Override
                        public void onVerificationResult(boolean isValid) {
                            if (isValid){
                                validationResults[2] = true;
                            } else {
                                validationResults[2] = false;
                            }

                            boolean allValid = true;
                            for (boolean result : validationResults){
                                if(!result){
                                    allValid = false;
                                    break;
                                }
                            }

                            if (allValid){
                                mAuth.createUserWithEmailAndPassword(email, password)
                                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                            @Override
                                            public void onComplete(@NonNull Task<AuthResult> task) {
                                                if (task.isSuccessful()) {
                                                    FirebaseUser user = mAuth.getCurrentUser();
                                                    if (user != null){
                                                        Map<String, Object> userData = new HashMap<>();
                                                        userData.put("userType", "business");
                                                        userData.put("userBusinessStatus", "pending");
                                                        userData.put("userEmail", email);
                                                        userData.put("userName", businessName);
                                                        userData.put("userAddress", businessAddress);
                                                        userData.put("userContact", contact);
                                                        userData.put("userPosCode", posCode);
                                                        userData.put("userDialCode", dialCode);
                                                        userData.put("userCountry", country);

                                                        Map<String, Object> requestData = new HashMap<>();
                                                        requestData.put("acceptRejectTime", null);
                                                        requestData.put("requestTime", FieldValue.serverTimestamp());
                                                        requestData.put("userId", user.getUid());

                                                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                                                        db.collection("user")
                                                                .document(user.getUid())
                                                                .set(userData)
                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void unused) {
                                                                        uploadToStorage("business", businessDocument, user.getUid(), businessProof);
                                                                        uploadToStorage("owner", ownerDocument, user.getUid(), ownerProof);

                                                                        DocumentReference counterRef = FirebaseFirestore.getInstance().collection("counter").document("request");
                                                                        counterRef.update("current", FieldValue.increment(1))
                                                                                .addOnSuccessListener(aVoid -> {
                                                                                    counterRef.get().addOnCompleteListener(task -> {
                                                                                        if (task.isSuccessful()){
                                                                                            DocumentSnapshot document = task.getResult();
                                                                                            if (document.exists()) {
                                                                                                Long newId = document.getLong("current");

                                                                                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                                                                                db.collection("request")
                                                                                                        .document(String.valueOf(newId))
                                                                                                        .set(requestData)
                                                                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                                            @Override
                                                                                                            public void onSuccess(Void unused) {
                                                                                                                progressDialog.dismiss();
                                                                                                                Toast.makeText(getApplicationContext(), "Successful Sign Up", Toast.LENGTH_SHORT).show();
                                                                                                                Intent loginIntent = new Intent(RegisterMain.this, LoginMain.class);
                                                                                                                startActivity(loginIntent);
                                                                                                                finish();
                                                                                                            }
                                                                                                        });
                                                                                            }
                                                                                        }
                                                                                    });
                                                                                });
                                                                    }
                                                                });
                                                    }
                                                } else {
                                                    progressDialog.dismiss();
                                                    Toast.makeText(RegisterMain.this, "Failed to sign up", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            } else {
                                progressDialog.dismiss();
                            }
                        }
                    });
                });
            });
        } else {
            progressDialog.dismiss();
        }
    }

    private void uploadToStorage(String businessOwner, File tempFile, String userID, String fileName){
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        Uri fileUri = Uri.fromFile(tempFile);
        StorageReference fileRef = storageRef.child("proofDocuments/" + userID + "/" + businessOwner + "/" + fileName);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri downloadUrl) {
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                Map<String, Object> proofData = new HashMap<>();
                                if (businessOwner.equals("business")) {
                                    proofData.put("businessProofUrl", downloadUrl);
                                } else if (businessOwner.equals("owner")){
                                    proofData.put("ownerProofUrl", downloadUrl);
                                }
                                db.collection("user")
                                        .document(userID)
                                        .set(proofData, SetOptions.merge());
                            }
                        });
                    }
                });
    }

    private void openCalendar(){
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog calendarDialog = new DatePickerDialog(this, R.style.custom_date_picker,
                new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int selectedYear, int selectedMonth, int selectedDay) {
                String formattedDate = String.format("%02d-%02d-%d", selectedDay, (selectedMonth + 1), selectedYear);
                dobInput.setText(formattedDate);
                setLayoutError(dobLayout, null);
            }
        }, year, month, day);

        calendarDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());
        calendarDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                // Set the button text color after the dialog is shown
                calendarDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(
                        ContextCompat.getColor(RegisterMain.this, R.color.teal_green));
                calendarDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(
                        ContextCompat.getColor(RegisterMain.this, R.color.teal_green));
            }
        });
        calendarDialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.register_main);

        accountTypeLayout = findViewById(R.id.account_type_input_layout);
        accountTypeAutoComplete = findViewById(R.id.account_type_input);
        dialCodeLayout = findViewById(R.id.dial_code_input_layout);
        dialCodeAutoComplete = findViewById(R.id.dial_code_input);
        countryLayout = findViewById(R.id.country_input_layout);
        countryAutoComplete = findViewById(R.id.country_input);
        contactLayout = findViewById(R.id.contact_input_layout);
        contactInput = findViewById(R.id.contact_input);
        posCodeLayout = findViewById(R.id.postal_code_input_layout);
        posCodeInput = findViewById(R.id.postal_code_input);
        passwordLayout = findViewById(R.id.password_input_layout);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordLayout = findViewById(R.id.confirm_password_input_layout);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        emailLayout = findViewById(R.id.email_input_layout);
        emailInput = findViewById(R.id.email_input);
        // Business Specific Info
        businessNameLayout = findViewById(R.id.business_name_input_layout);
        businessNameInput = findViewById(R.id.business_name_input);
        businessAddressLayout = findViewById(R.id.business_address_input_layout);
        businessAddressInput = findViewById(R.id.business_address_input);
        businessProofLayout = findViewById(R.id.business_proof_input_layout);
        businessProofInput = findViewById(R.id.business_proof_input);
        ownerProofLayout = findViewById(R.id.owner_proof_input_layout);
        ownerProofInput = findViewById(R.id.owner_proof_input);
        // Customer Specific Info
        nameLayout = findViewById(R.id.name_input_layout);
        nameInput = findViewById(R.id.name_input);
        addressLayout = findViewById(R.id.address_input_layout);
        addressInput = findViewById(R.id.address_input);
        dobLayout = findViewById(R.id.dob_input_layout);
        dobInput = findViewById(R.id.dob_input);
        genderLayout = findViewById(R.id.gender_input_layout);
        genderAutoComplete = findViewById(R.id.gender_input);
        // Button
        doneButton = findViewById(R.id.done_button);
        loginButton = findViewById(R.id.login_button);

        agreementText = findViewById(R.id.agreement_text);

        businessDocument = null;
        ownerDocument = null;

        mAuth = FirebaseAuth.getInstance();

        String[] accountType = getResources().getStringArray(R.array.account_array);
        accountTypeAdapter = new ArrayAdapter<>(this, R.layout.dropdown_menu, accountType);
        accountTypeAutoComplete.setAdapter(accountTypeAdapter);

        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        List<String> dialCodes = new ArrayList<>();
        for (String regionCode : phoneNumberUtil.getSupportedRegions()) {
            int dialCode = phoneNumberUtil.getCountryCodeForRegion(regionCode);
            String regionName = new Locale("", regionCode).getDisplayCountry();
            dialCodes.add("+" + dialCode + " (" + regionName + ")");
        }
        dialCodes.sort(Comparator.comparing(code -> code.split(" \\(")[1]));
        dialCodeAdapter = new ArrayAdapter<>(this, R.layout.dropdown_menu, dialCodes);
        dialCodeAutoComplete.setAdapter(dialCodeAdapter);

        List<String> countries = new ArrayList<>();
        for (String countryCode : Locale.getISOCountries()) {
            Locale locale = new Locale("", countryCode);
            countries.add(locale.getDisplayCountry());
        }
        Collections.sort(countries);
        countryAdapter = new ArrayAdapter<>(this, R.layout.dropdown_menu, countries);
        countryAutoComplete.setAdapter(countryAdapter);

        String[] gender = getResources().getStringArray(R.array.gender_array);
        genderAdapter = new ArrayAdapter<>(this, R.layout.dropdown_menu, gender);
        genderAutoComplete.setAdapter(genderAdapter);

        accountTypeAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l){
                setLayoutError(accountTypeLayout, null);
                String account = adapterView.getItemAtPosition(i).toString();

                dialCodeLayout.setVisibility(View.VISIBLE);
                contactLayout.setVisibility(View.VISIBLE);
                posCodeLayout.setVisibility(View.VISIBLE);
                countryLayout.setVisibility(View.VISIBLE);
                passwordLayout.setVisibility(View.VISIBLE);
                confirmPasswordLayout.setVisibility(View.VISIBLE);
                emailLayout.setVisibility(View.VISIBLE);
                agreementText.setVisibility(View.VISIBLE);


                if (account.equals("Business")){
                    clearField();
                    businessNameLayout.setVisibility(View.VISIBLE);
                    businessAddressLayout.setVisibility(View.VISIBLE);
                    businessProofLayout.setVisibility(View.VISIBLE);
                    ownerProofLayout.setVisibility(View.VISIBLE);

                    doneButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            View focusedView = getCurrentFocus();
                            if (focusedView != null){
                                focusedView.clearFocus();
                                InputMethodManager imm = (InputMethodManager) getSystemService(RegisterMain.INPUT_METHOD_SERVICE);
                                if (imm != null) {
                                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                                }
                            }
                            businessRegister();
                        }
                    });
                } else if (account.equals("Customer")){
                    clearField();
                    nameLayout.setVisibility(View.VISIBLE);
                    addressLayout.setVisibility(View.VISIBLE);
                    dobLayout.setVisibility(View.VISIBLE);
                    genderLayout.setVisibility(View.VISIBLE);

                    doneButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            View focusedView = getCurrentFocus();
                            if (focusedView != null){
                                focusedView.clearFocus();
                                InputMethodManager imm = (InputMethodManager) getSystemService(RegisterMain.INPUT_METHOD_SERVICE);
                                if (imm != null) {
                                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                                }
                            }
                            customerRegister();
                        }
                    });
                }
            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View focusedView = getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(RegisterMain.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }
                String account = accountTypeAutoComplete.getText().toString().trim();
                if (account.isEmpty()){
                    setLayoutError(accountTypeLayout, "Required*");
                }
            }
        });

        dobLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View focusedView = getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(RegisterMain.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }
                openCalendar();
            }
        });

        businessProofLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View focusedView = getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(RegisterMain.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }
                uploadBusinessDocument();
            }
        });

        ownerProofLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View focusedView = getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(RegisterMain.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }
                uploadCustomerDocument();
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent loginIntent = new Intent(RegisterMain.this, LoginMain.class);
                startActivity(loginIntent);
                finish();
            }
        });

        accountTypeAutoComplete.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    View focusedView = getCurrentFocus();
                    if (focusedView != null){
                        focusedView.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(RegisterMain.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                        }
                    }
                    setLayoutError(accountTypeLayout, null);
                }
            }
        });

        dialCodeAutoComplete.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    View focusedView = getCurrentFocus();
                    if (focusedView != null){
                        focusedView.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(RegisterMain.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                        }
                    }
                    setLayoutError(dialCodeLayout, null);
                }
            }
        });

        countryAutoComplete.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    View focusedView = getCurrentFocus();
                    if (focusedView != null){
                        focusedView.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(RegisterMain.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                        }
                    }
                    setLayoutError(countryLayout, null);
                }
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

        confirmPasswordInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    setLayoutError(confirmPasswordLayout, null);
                }
            }
        });

        contactInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    setLayoutError(contactLayout, null);
                }
            }
        });

        posCodeInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    setLayoutError(posCodeLayout, null);
                }
            }
        });

        businessNameInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    setLayoutError(businessNameLayout, null);
                }
            }
        });

        businessAddressInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    setLayoutError(businessAddressLayout, null);
                }
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

        addressInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    setLayoutError(addressLayout, null);
                }
            }
        });

        genderAutoComplete.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    View focusedView = getCurrentFocus();
                    if (focusedView != null){
                        focusedView.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(RegisterMain.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                        }
                    }
                    setLayoutError(genderLayout, null);
                }
            }
        });
    }
}