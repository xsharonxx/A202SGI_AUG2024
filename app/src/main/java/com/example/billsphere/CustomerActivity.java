package com.example.billsphere;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.Objects;

import javax.annotation.Nonnull;

public class CustomerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener{

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navigationView;
    private ImageView userImage;
    private TextView userName;
    private ImageButton qrButton;

    private Bitmap generateQRCode(String email) {
        try {
            // Size of the QR code
            int size = 500;
            // Create a BitMatrix for the QR code
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    email, BarcodeFormat.QR_CODE, size, size, null);

            // Create a bitmap
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);

            int mainColor = ContextCompat.getColor(getApplicationContext(), R.color.teal_green);
            int emptyColor = ContextCompat.getColor(getApplicationContext(), R.color.light_green);

            // Fill the bitmap based on the matrix
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? mainColor : emptyColor);
                }
            }

            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void updateImageName(){
        SharedPreferences preferences1 = getSharedPreferences("user", MODE_PRIVATE);
        String user = preferences1.getString("uid", "");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("user")
                .document(user)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                String image = document.getString("userImage");
                                String name = document.getString("userName");

                                if (image != null){
                                    Glide.with(CustomerActivity.this).load(image)
                                            .apply(RequestOptions.circleCropTransform()).into(userImage);
                                } else {
                                    userImage.setImageResource(R.drawable.null_profile_image);
                                }
                                userName.setText(name);
                            }
                        }
                    }
                });
    }

    @Override
    protected void onResume(){
        super.onResume();
        updateImageName();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.customer_activity);

        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        setSupportActionBar(toolbar);

        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toggle.getDrawerArrowDrawable().setColor(ContextCompat.getColor(this, R.color.white));

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new F1Sellers()).commit();
        navigationView.setCheckedItem(R.id.sellers);

        // Change the system bar color
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));

        // Handle back press with OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    // Call default back action
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        View headerView = navigationView.getHeaderView(0);
        userImage = headerView.findViewById(R.id.user_image);
        userName = headerView.findViewById(R.id.user_name);
        qrButton = headerView.findViewById(R.id.qr_code_button);

        updateImageName();

        userImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CustomerActivity.this, ProfileCustomer.class);
                startActivity(intent);
            }
        });

        userName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CustomerActivity.this, ProfileCustomer.class);
                startActivity(intent);
            }
        });

        qrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences preferences1 = getSharedPreferences("user", MODE_PRIVATE);
                String user = preferences1.getString("uid", "");
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("user")
                        .document(user)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document != null && document.exists()) {
                                        String email = document.getString("userEmail");

                                        Bitmap qrCodeBitmap = generateQRCode(email);

                                        Dialog dialog = new Dialog(CustomerActivity.this);
                                        dialog.setContentView(R.layout.qr_dialog);
                                        dialog.setCanceledOnTouchOutside(true);
                                        dialog.setCancelable(true);

                                        ImageView qrCodeImageView = dialog.findViewById(R.id.qr_code_image_view);
                                        TextView qrEmail = dialog.findViewById(R.id.qr_email);

                                        qrCodeImageView.setImageBitmap(qrCodeBitmap);
                                        qrEmail.setText(email);

                                        dialog.show();
                                    }
                                }
                            }
                        });
            }
        });
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onNavigationItemSelected(@Nonnull MenuItem item){
        // Highlight the selected item
        item.setChecked(true);
        int id = item.getItemId();
        if (id == R.id.bill_history) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new F1BillHistory()).commitNow();
        } else if (id == R.id.sellers) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new F1Sellers()).commitNow();
        } else if (id == R.id.logout) {
            logout();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        invalidateOptionsMenu();
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        // Dynamically show the correct icon based on the active fragment
        MenuItem billHistoryIcon = menu.findItem(R.id.bill_history_icon);
        MenuItem sellersIcon = menu.findItem(R.id.sellers_icon);

        billHistoryIcon.setVisible(false);
        sellersIcon.setVisible(false);

        // Check which fragment is currently displayed and show the appropriate icon
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        // Show icon when in fragment
        if (currentFragment instanceof F1BillHistory) {
            billHistoryIcon.setVisible(true);
            Objects.requireNonNull(billHistoryIcon.getIcon()).setTint(ContextCompat.getColor(this, R.color.white));
        } else if (currentFragment instanceof F1Sellers){
            sellersIcon.setVisible(true);
            Objects.requireNonNull(sellersIcon.getIcon()).setTint(ContextCompat.getColor(this, R.color.white));
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu that contains icons for the right side of the toolbar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.customer_toolbar_menu, menu);
        return true;
    }

    private void logout(){
        SharedPreferences preferences = getSharedPreferences("user", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        SharedPreferences preferences1 = getSharedPreferences("checkbox", MODE_PRIVATE);
        SharedPreferences.Editor editor1 = preferences1.edit();
        editor1.clear();
        editor1.apply();

        Intent intent = new Intent(CustomerActivity.this, LoginMain.class);
        startActivity(intent);
        finish();
    }
}