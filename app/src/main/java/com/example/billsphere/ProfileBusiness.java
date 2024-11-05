package com.example.billsphere;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import org.json.JSONArray;
import org.json.JSONObject;

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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import android.os.Environment;
import android.provider.MediaStore;
import android.content.ContentResolver;

public class ProfileBusiness extends AppCompatActivity {

    private ImageButton backButton;
    private TextView userEmail;
    private ImageView userImage;
    private AutoCompleteTextView dialInput, countryInput;
    private TextInputEditText nameInput, contactInput, posInput, addressInput;
    private TextInputLayout nameLayout, dialLayout, contactLayout, countryLayout, posLayout, addressLayout;
    private TextInputLayout businessProofLayout, ownerProofLayout;
    private TextInputEditText businessProofInput, ownerProofInput;
    private TextView changePasswordButton;
    private Button saveButton;

    private ListenerRegistration listenerRegistration;

    private int disableColor;

    private File businessDocument, ownerDocument;
    private File userImageFile;

    private ArrayAdapter<String> dialCodeAdapter, countryAdapter;

    private boolean isImageUploaded;
    private boolean isBusinessDocUploaded;
    private boolean isOwnerDocUploaded;

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

    private void sendChangePassword(){
        ProgressDialog progressDialog = new ProgressDialog();
        progressDialog.show(getSupportFragmentManager(), "progressDialog");
        String email = userEmail.getText().toString().trim();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.sendPasswordResetEmail(email).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                progressDialog.dismiss();
                Toast.makeText(ProfileBusiness.this, "Check Your Email for Reset Password",
                        Toast.LENGTH_LONG).show();
            }
        });
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
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(contact, isoCountry);
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

    private void uploadToStorage(File tempFile, String userID, OnUserImageUploadedListener listener) {
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

    private final ActivityResultLauncher<Intent> pickBusinessPdfLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        String filename = getFileName(fileUri);
                        if (filename != null) {
                            businessProofInput.setText(filename);
                            businessProofInput.setPaintFlags(businessProofInput.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
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
                            ownerProofInput.setPaintFlags(ownerProofInput.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
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

    private void openPdf(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private void downloadAndOpenPDF(String proofUrl, String fileName) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReferenceFromUrl(proofUrl);

        storageReference.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // File downloaded successfully, now store it in Downloads
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                ContentResolver resolver = getContentResolver();
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    try {
                        OutputStream outputStream = resolver.openOutputStream(uri);
                        if (outputStream != null) {
                            outputStream.write(bytes);
                            outputStream.close();
                            Toast.makeText(getApplicationContext(), "Download successful", Toast.LENGTH_SHORT).show();
                            openPdf(uri);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Failed to save PDF", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void uploadDocumentToStorage(String businessOwner, File tempFile, String userID, String fileName, OnDocumentUploadedListener listener){
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
                                listener.onDocumentUploaded();
                            }
                        });
                    }
                });
    }

    public interface OnUserImageUploadedListener {
        void onImageUploaded();
    }

    public interface OnDocumentUploadedListener {
        void onDocumentUploaded();
    }

    private void validateSave(){
        ProgressDialog progressDialog = new ProgressDialog();
        progressDialog.show(getSupportFragmentManager(), "progressDialog");
        String name = nameInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String contact = contactInput.getText().toString().trim();
        String posCode = posInput.getText().toString().trim();
        String dialCode = dialInput.getText().toString().trim();
        String country = countryInput.getText().toString().trim();
        String businessDoc = businessProofInput.getText().toString().trim();
        String ownerDoc = ownerProofInput.getText().toString().trim();

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
        if (contact.isEmpty()) {
            setLayoutError(contactLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(contactLayout, null);
        }
        if (dialCode.isEmpty()) {
            setLayoutError(dialLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(dialLayout, null);
        }
        if (country.isEmpty()) {
            setLayoutError(countryLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(countryLayout, null);
        }
        if (posCode.isEmpty()) {
            setLayoutError(posLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(posLayout, null);
        }
        if (businessDoc.isEmpty()) {
            setLayoutError(businessProofLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(businessProofLayout, null);
        }
        if (ownerDoc.isEmpty()) {
            setLayoutError(ownerProofLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(ownerProofLayout, null);
        }

        if (valid){
            boolean[] validationResults = new boolean[2];

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
                        setLayoutError(posLayout, null);
                    } else {
                        setLayoutError(posLayout, "Invalid Pos Code");
                    }

                    boolean allValid = true;
                    for (boolean result : validationResults){
                        if(!result){
                            allValid = false;
                            break;
                        }
                    }

                    if (allValid) {
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("userName", name);
                        userData.put("userAddress", address);
                        userData.put("userContact", contact);
                        userData.put("userPosCode", posCode);
                        userData.put("userDialCode", dialCode);
                        userData.put("userCountry", country);

                        SharedPreferences preferences1 = getSharedPreferences("user", MODE_PRIVATE);
                        String user = preferences1.getString("uid", "");

                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("user")
                                .document(user)
                                .update(userData)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        uploadFiles(user, userImageFile, businessDocument, businessDoc,
                                                ownerDocument, ownerDoc, progressDialog, "Edit");
                                    }
                                });
                    } else {
                        progressDialog.dismiss();
                    }
                });
            });
        } else {
            progressDialog.dismiss();
        }
    }

    private void checkAllUploadsCompleted(ProgressDialog progressDialog, String editResubmit) {
        if (isImageUploaded && isBusinessDocUploaded && isOwnerDocUploaded) {
            progressDialog.dismiss();
            // All uploads completed
            Toast.makeText(getApplicationContext(), "Successful " + editResubmit, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void uploadFiles(String userId, File userImageFile, File businessDocument, String businessFileName,
                             File ownerDocument, String ownerFileName, ProgressDialog progressDialog, String editResubmit) {
        if (userImageFile != null) {
            uploadToStorage(userImageFile, userId, new OnUserImageUploadedListener() {
                @Override
                public void onImageUploaded() {
                    isImageUploaded = true;
                    checkAllUploadsCompleted(progressDialog, editResubmit);
                }
            });
        } else {
            isImageUploaded = true;  // If nothing to upload, consider it done
            checkAllUploadsCompleted(progressDialog, editResubmit);
        }

        if (businessDocument != null) {
            uploadDocumentToStorage("business", businessDocument, userId, businessFileName, new OnDocumentUploadedListener() {
                @Override
                public void onDocumentUploaded() {
                    isBusinessDocUploaded = true;
                    checkAllUploadsCompleted(progressDialog, editResubmit);
                }
            });
        } else {
            isBusinessDocUploaded = true;
            checkAllUploadsCompleted(progressDialog, editResubmit);
        }

        if (ownerDocument != null) {
            uploadDocumentToStorage("owner", ownerDocument, userId, ownerFileName, new OnDocumentUploadedListener() {
                @Override
                public void onDocumentUploaded() {
                    isOwnerDocUploaded = true;
                    checkAllUploadsCompleted(progressDialog, editResubmit);
                }
            });
        } else {
            isOwnerDocUploaded = true;
            checkAllUploadsCompleted(progressDialog, editResubmit);
        }
    }

    private void validateResubmit(){
        ProgressDialog progressDialog = new ProgressDialog();
        progressDialog.show(getSupportFragmentManager(), "progressDialog");
        String name = nameInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String contact = contactInput.getText().toString().trim();
        String posCode = posInput.getText().toString().trim();
        String dialCode = dialInput.getText().toString().trim();
        String country = countryInput.getText().toString().trim();
        String businessDoc = businessProofInput.getText().toString().trim();
        String ownerDoc = ownerProofInput.getText().toString().trim();

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
        if (contact.isEmpty()) {
            setLayoutError(contactLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(contactLayout, null);
        }
        if (dialCode.isEmpty()) {
            setLayoutError(dialLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(dialLayout, null);
        }
        if (country.isEmpty()) {
            setLayoutError(countryLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(countryLayout, null);
        }
        if (posCode.isEmpty()) {
            setLayoutError(posLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(posLayout, null);
        }
        if (businessDoc.isEmpty()) {
            setLayoutError(businessProofLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(businessProofLayout, null);
        }
        if (ownerDoc.isEmpty()) {
            setLayoutError(ownerProofLayout, "Required*");
            valid = false;
        } else {
            setLayoutError(ownerProofLayout, null);
        }

        if (valid){
            boolean[] validationResults = new boolean[2];

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
                        setLayoutError(posLayout, null);
                    } else {
                        setLayoutError(posLayout, "Invalid Pos Code");
                    }

                    boolean allValid = true;
                    for (boolean result : validationResults){
                        if(!result){
                            allValid = false;
                            break;
                        }
                    }

                    if (allValid) {
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("userName", name);
                        userData.put("userAddress", address);
                        userData.put("userContact", contact);
                        userData.put("userPosCode", posCode);
                        userData.put("userDialCode", dialCode);
                        userData.put("userCountry", country);
                        userData.put("userBusinessStatus", "pending");

                        SharedPreferences preferences1 = getSharedPreferences("user", MODE_PRIVATE);
                        String user = preferences1.getString("uid", "");

                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("user")
                                .document(user)
                                .update(userData)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        DocumentReference counterRef = FirebaseFirestore.getInstance().collection("counter").document("request");
                                        counterRef.update("current", FieldValue.increment(1))
                                                .addOnSuccessListener(aVoid -> {
                                                    counterRef.get().addOnCompleteListener(task -> {
                                                        if (task.isSuccessful()){
                                                            DocumentSnapshot document = task.getResult();
                                                            if (document.exists()) {
                                                                Long newId = document.getLong("current");

                                                                SharedPreferences preferences1 = getSharedPreferences("user", MODE_PRIVATE);
                                                                String user = preferences1.getString("uid", "");

                                                                Map<String, Object> requestData = new HashMap<>();
                                                                requestData.put("acceptRejectTime", null);
                                                                requestData.put("requestTime", FieldValue.serverTimestamp());
                                                                requestData.put("userId", user);

                                                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                                                db.collection("request")
                                                                        .document(String.valueOf(newId))
                                                                        .set(requestData)
                                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                            @Override
                                                                            public void onSuccess(Void unused) {
                                                                                SharedPreferences preferences = getSharedPreferences("businessStatus", MODE_PRIVATE);
                                                                                SharedPreferences.Editor editor = preferences.edit();
                                                                                editor.putString("status", "pending");
                                                                                editor.apply();
                                                                                uploadFiles(user, userImageFile, businessDocument, businessDoc,
                                                                                        ownerDocument, ownerDoc, progressDialog, "Resubmit Request");
                                                                            }
                                                                        });
                                                            }
                                                        }
                                                    });
                                                });
                                    }
                                });
                    } else {
                        progressDialog.dismiss();
                    }
                });
            });
        } else {
            progressDialog.dismiss();
        }
    }

    private void setupSnapshotListener(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        listenerRegistration = db.collection("user").document(userId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.w("Firestore", "Listen failed.", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String userBusinessStatus = documentSnapshot.getString("userBusinessStatus");

                        // Check for re-login conditions
                        if ("accepted".equals(userBusinessStatus) || "rejected".equals(userBusinessStatus)) {
                            finish();
                        }
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.profile_business);

        SharedPreferences preferences1 = getSharedPreferences("user", MODE_PRIVATE);
        String user = preferences1.getString("uid", "");
        SharedPreferences preferences = getSharedPreferences("businessStatus", Context.MODE_PRIVATE);
        String status = preferences.getString("status", "");
        if (status.equals("pending")) {
            setupSnapshotListener(user);
        }

        backButton = findViewById(R.id.back_button);
        userEmail = findViewById(R.id.email);
        userImage = findViewById(R.id.image);
        nameLayout = findViewById(R.id.name_layout);
        nameInput = findViewById(R.id.name_input);
        dialLayout = findViewById(R.id.dial_code_input_layout);
        dialInput = findViewById(R.id.dial_code_input);
        contactLayout = findViewById(R.id.contact_input_layout);
        contactInput = findViewById(R.id.contact_input);
        countryLayout = findViewById(R.id.country_input_layout);
        countryInput = findViewById(R.id.country_input);
        posLayout = findViewById(R.id.postal_code_input_layout);
        posInput = findViewById(R.id.postal_code_input);
        addressLayout = findViewById(R.id.address_input_layout);
        addressInput = findViewById(R.id.address_input);
        businessProofInput = findViewById(R.id.business_proof_input);
        businessProofLayout = findViewById(R.id.business_proof_input_layout);
        ownerProofInput = findViewById(R.id.owner_proof_input);
        ownerProofLayout = findViewById(R.id.owner_proof_input_layout);
        changePasswordButton = findViewById(R.id.change_password_button);
        saveButton = findViewById(R.id.save_button);

        userImageFile = null;
        businessDocument = null;
        ownerDocument = null;

        isImageUploaded = false;
        isBusinessDocUploaded = false;
        isOwnerDocUploaded = false;

        disableColor = ContextCompat.getColor(this, R.color.disable_button);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("user").document(user);
        userRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    String name = documentSnapshot.getString("userName");
                    String email = documentSnapshot.getString("userEmail");
                    String image = documentSnapshot.getString("userImage");
                    String address = documentSnapshot.getString("userAddress");
                    String pos = documentSnapshot.getString("userPosCode");
                    String country = documentSnapshot.getString("userCountry");
                    String dial = documentSnapshot.getString("userDialCode");
                    String contact = documentSnapshot.getString("userContact");
                    String businessProof = documentSnapshot.getString("businessProofUrl");
                    String ownerProof = documentSnapshot.getString("ownerProofUrl");

                    String businessWithoutParams = businessProof.split("\\?")[0];
                    String businessDecodedUrl = Uri.decode(businessWithoutParams);
                    String businessFileName = businessDecodedUrl.substring(businessDecodedUrl.lastIndexOf("/") + 1);

                    String ownerWithoutParams = ownerProof.split("\\?")[0];
                    String ownerDecodedUrl = Uri.decode(ownerWithoutParams);
                    String ownerFileName = ownerDecodedUrl.substring(ownerDecodedUrl.lastIndexOf("/") + 1);

                    businessProofInput.setText(businessFileName);
                    businessProofInput.setPaintFlags(businessProofInput.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                    businessProofInput.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            View focusedView = getCurrentFocus();
                            if (focusedView != null){
                                focusedView.clearFocus();
                                InputMethodManager imm = (InputMethodManager) getSystemService(ProfileBusiness.INPUT_METHOD_SERVICE);
                                if (imm != null) {
                                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                                }
                            }
                            if (businessDocument == null){
                                downloadAndOpenPDF(businessProof, businessFileName);
                            }
                        }
                    });

                    ownerProofInput.setText(ownerFileName);
                    ownerProofInput.setPaintFlags(ownerProofInput.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                    ownerProofInput.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            View focusedView = getCurrentFocus();
                            if (focusedView != null){
                                focusedView.clearFocus();
                                InputMethodManager imm = (InputMethodManager) getSystemService(ProfileBusiness.INPUT_METHOD_SERVICE);
                                if (imm != null) {
                                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                                }
                            }
                            if (ownerDocument == null){
                                downloadAndOpenPDF(ownerProof, ownerFileName);
                            }
                        }
                    });

                    userEmail.setText(email);

                    nameInput.setText(name);
                    addressInput.setText(address);
                    posInput.setText(pos);
                    countryInput.setText(country);
                    dialInput.setText(dial);
                    contactInput.setText(contact);

                    if (image != null){
                        Glide.with(ProfileBusiness.this).load(image)
                                .apply(RequestOptions.circleCropTransform()).into(userImage);
                    } else {
                        userImage.setImageResource(R.drawable.null_profile_image);
                    }

                    PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
                    List<String> dialCodes = new ArrayList<>();
                    for (String regionCode : phoneNumberUtil.getSupportedRegions()) {
                        int dialCode = phoneNumberUtil.getCountryCodeForRegion(regionCode);
                        String regionName = new Locale("", regionCode).getDisplayCountry();
                        dialCodes.add("+" + dialCode + " (" + regionName + ")");
                    }
                    dialCodes.sort(Comparator.comparing(code -> code.split(" \\(")[1]));
                    dialCodeAdapter = new ArrayAdapter<>(ProfileBusiness.this, R.layout.dropdown_menu, dialCodes);
                    dialInput.setAdapter(dialCodeAdapter);

                    List<String> countries = new ArrayList<>();
                    for (String countryCode : Locale.getISOCountries()) {
                        Locale locale = new Locale("", countryCode);
                        countries.add(locale.getDisplayCountry());
                    }
                    Collections.sort(countries);
                    countryAdapter = new ArrayAdapter<>(ProfileBusiness.this, R.layout.dropdown_menu, countries);
                    countryInput.setAdapter(countryAdapter);

                    SharedPreferences preferences = getSharedPreferences("businessStatus", Context.MODE_PRIVATE);
                    String status = preferences.getString("status", "");
                    if (status.equals("accepted")){
                        saveButton.setVisibility(View.VISIBLE);
                        saveButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                View focusedView = getCurrentFocus();
                                if (focusedView != null){
                                    focusedView.clearFocus();
                                    InputMethodManager imm = (InputMethodManager) getSystemService(ProfileBusiness.INPUT_METHOD_SERVICE);
                                    if (imm != null) {
                                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                                    }
                                }
                                validateSave();
                            }
                        });
                    } else if (status.equals("rejected")){
                        saveButton.setText("Resubmit");
                        saveButton.setVisibility(View.VISIBLE);
                        saveButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                View focusedView = getCurrentFocus();
                                if (focusedView != null){
                                    focusedView.clearFocus();
                                    InputMethodManager imm = (InputMethodManager) getSystemService(ProfileBusiness.INPUT_METHOD_SERVICE);
                                    if (imm != null) {
                                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                                    }
                                }
                                validateResubmit();
                            }
                        });
                    } else if (status.equals("pending")){
                        saveButton.setEnabled(false);
                        saveButton.setBackgroundColor(disableColor);
                        saveButton.setText(R.string.request_in_progress);
                        saveButton.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        businessProofLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View focusedView = getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(ProfileBusiness.INPUT_METHOD_SERVICE);
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
                    InputMethodManager imm = (InputMethodManager) getSystemService(ProfileBusiness.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }
                uploadCustomerDocument();
            }
        });

        userImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View focusedView = getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(ProfileBusiness.INPUT_METHOD_SERVICE);
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
                    InputMethodManager imm = (InputMethodManager) getSystemService(ProfileBusiness.INPUT_METHOD_SERVICE);
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

        dialInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    View focusedView = getCurrentFocus();
                    if (focusedView != null){
                        focusedView.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(ProfileBusiness.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                        }
                    }
                    setLayoutError(dialLayout, null);
                }
            }
        });

        countryInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    View focusedView = getCurrentFocus();
                    if (focusedView != null){
                        focusedView.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(ProfileBusiness.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                        }
                    }
                    setLayoutError(countryLayout, null);
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

        posInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    setLayoutError(posLayout, null);
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
