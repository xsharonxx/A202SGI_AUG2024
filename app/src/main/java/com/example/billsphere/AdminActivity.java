package com.example.billsphere;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

import javax.annotation.Nonnull;

public class AdminActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navigationView;
    private ImageView userImage;
    private TextView userName;

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
                                    Glide.with(AdminActivity.this).load(image)
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.admin_activity);

        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        setSupportActionBar(toolbar);

        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toggle.getDrawerArrowDrawable().setColor(ContextCompat.getColor(this, R.color.white));

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new F1Requests()).commit();
        navigationView.setCheckedItem(R.id.requests);

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

        updateImageName();

        userImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AdminActivity.this, ProfileAdmin.class);
                startActivity(intent);
            }
        });

        userName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AdminActivity.this, ProfileAdmin.class);
                startActivity(intent);
            }
        });
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onNavigationItemSelected(@Nonnull MenuItem item){
        // Highlight the selected item
        item.setChecked(true);
        int id = item.getItemId();
        if (id == R.id.requests) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new F1Requests()).commitNow();
        } else if (id == R.id.sellers) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new F1SellersAdmin()).commitNow();
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
        MenuItem requestIcon = menu.findItem(R.id.request_icon);
        MenuItem sellersIcon = menu.findItem(R.id.sellers_icon);

        requestIcon.setVisible(false);
        sellersIcon.setVisible(false);

        // Check which fragment is currently displayed and show the appropriate icon
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        // Show icon when in fragment
        if (currentFragment instanceof F1Requests) {
            requestIcon.setVisible(true);
            Objects.requireNonNull(requestIcon.getIcon()).setTint(ContextCompat.getColor(this, R.color.white));
        } else if (currentFragment instanceof F1SellersAdmin){
            sellersIcon.setVisible(true);
            Objects.requireNonNull(sellersIcon.getIcon()).setTint(ContextCompat.getColor(this, R.color.white));
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu that contains icons for the right side of the toolbar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.admin_toolbar_menu, menu);
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

        Intent intent = new Intent(AdminActivity.this, LoginMain.class);
        startActivity(intent);
        finish();
    }


}