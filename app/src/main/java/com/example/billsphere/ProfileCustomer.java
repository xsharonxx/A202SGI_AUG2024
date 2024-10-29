package com.example.billsphere;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
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

public class ProfileCustomer extends AppCompatActivity {

    private ImageButton backButton;
    private TextView userEmail;
    private ImageView userImage;
    private AutoCompleteTextView dialInput, countryInput, genderInput;
    private TextInputEditText nameInput, contactInput, posInput, addressInput, dobInput;
    private TextInputLayout nameLayout, dialLayout, contactLayout, countryLayout, posLayout, addressLayout;
    private TextInputLayout genderLayout, dobLayout;
    private TextView changePasswordButton;
    private Button saveButton;
    private File userImageFile;

    private ArrayAdapter<String> dialCodeAdapter, countryAdapter, genderAdapter;

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
                        ContextCompat.getColor(ProfileCustomer.this, R.color.teal_green));
                calendarDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(
                        ContextCompat.getColor(ProfileCustomer.this, R.color.teal_green));
            }
        });
        calendarDialog.show();
    }

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

    private void sendChangePassword(){
        ProgressDialog progressDialog = new ProgressDialog();
        progressDialog.show(getSupportFragmentManager(), "progressDialog");
        String email = userEmail.getText().toString().trim();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.sendPasswordResetEmail(email).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                progressDialog.dismiss();
                Toast.makeText(ProfileCustomer.this, "Check Your Email for Reset Password",
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
        String address = addressInput.getText().toString().trim();
        String dob = dobInput.getText().toString().trim();
        String gender = genderInput.getText().toString().trim();
        String contact = contactInput.getText().toString().trim();
        String posCode = posInput.getText().toString().trim();
        String dialCode = dialInput.getText().toString().trim();
        String country = countryInput.getText().toString().trim();

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
                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                        try {
                            Date dateOfBirth = sdf.parse(dob);
                            Timestamp dobTimestamp = new Timestamp(dateOfBirth);

                            Map<String, Object> userData = new HashMap<>();
                            userData.put("userName", name);
                            userData.put("userAddress", address);
                            userData.put("userDOB", dobTimestamp);
                            userData.put("userGender", gender);
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
                                            if (userImageFile != null) {
                                                uploadToStorage(userImageFile, user, new ProfileCustomer.OnUserImageUploadedListener() {
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
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } else {
                        progressDialog.dismiss();
                    }
                });
            });
        } else {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.profile_customer);

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
        genderLayout = findViewById(R.id.gender_input_layout);
        genderInput = findViewById(R.id.gender_input);
        dobLayout = findViewById(R.id.dob_input_layout);
        dobInput = findViewById(R.id.dob_input);
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
                    String address = documentSnapshot.getString("userAddress");
                    String pos = documentSnapshot.getString("userPosCode");
                    String country = documentSnapshot.getString("userCountry");
                    String dial = documentSnapshot.getString("userDialCode");
                    String contact = documentSnapshot.getString("userContact");
                    String gender = documentSnapshot.getString("userGender");
                    Timestamp timestamp = documentSnapshot.getTimestamp("userDOB");
                    LocalDateTime localDateTime = null;
                    if (timestamp != null) {
                        localDateTime = timestamp.toDate()
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime();
                    }

                    userEmail.setText(email);

                    nameInput.setText(name);
                    addressInput.setText(address);
                    posInput.setText(pos);
                    countryInput.setText(country);
                    dialInput.setText(dial);
                    contactInput.setText(contact);
                    genderInput.setText(gender);

                    if (image != null){
                        Glide.with(ProfileCustomer.this).load(image)
                                .apply(RequestOptions.circleCropTransform()).into(userImage);
                    } else {
                        userImage.setImageResource(R.drawable.null_profile_image);
                    }

                    DateTimeFormatter datetimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                    String datetime = localDateTime.format(datetimeFormatter);
                    dobInput.setText(datetime);

                    PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
                    List<String> dialCodes = new ArrayList<>();
                    for (String regionCode : phoneNumberUtil.getSupportedRegions()) {
                        int dialCode = phoneNumberUtil.getCountryCodeForRegion(regionCode);
                        String regionName = new Locale("", regionCode).getDisplayCountry();
                        dialCodes.add("+" + dialCode + " (" + regionName + ")");
                    }
                    dialCodes.sort(Comparator.comparing(code -> code.split(" \\(")[1]));
                    dialCodeAdapter = new ArrayAdapter<>(ProfileCustomer.this, R.layout.dropdown_menu, dialCodes);
                    dialInput.setAdapter(dialCodeAdapter);

                    List<String> countries = new ArrayList<>();
                    for (String countryCode : Locale.getISOCountries()) {
                        Locale locale = new Locale("", countryCode);
                        countries.add(locale.getDisplayCountry());
                    }
                    Collections.sort(countries);
                    countryAdapter = new ArrayAdapter<>(ProfileCustomer.this, R.layout.dropdown_menu, countries);
                    countryInput.setAdapter(countryAdapter);

                    String[] genderList = getResources().getStringArray(R.array.gender_array);
                    genderAdapter = new ArrayAdapter<>(ProfileCustomer.this, R.layout.dropdown_menu, genderList);
                    genderInput.setAdapter(genderAdapter);
                }
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
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
                validateSave();
            }
        });

        userImage.setOnClickListener(new View.OnClickListener() {
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

        dobLayout.setEndIconOnClickListener(new View.OnClickListener() {
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
                openCalendar();
            }
        });

        dialInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    View focusedView = getCurrentFocus();
                    if (focusedView != null){
                        focusedView.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(ProfileCustomer.INPUT_METHOD_SERVICE);
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
                        InputMethodManager imm = (InputMethodManager) getSystemService(ProfileCustomer.INPUT_METHOD_SERVICE);
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

        genderInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    View focusedView = getCurrentFocus();
                    if (focusedView != null){
                        focusedView.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(ProfileCustomer.INPUT_METHOD_SERVICE);
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
