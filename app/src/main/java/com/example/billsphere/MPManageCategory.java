package com.example.billsphere;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MPManageCategory extends AppCompatActivity implements CategoryAdapter.OnCategoryUpdateListener {

    private ImageButton backButton;
    private SearchView searchBar;
    private ImageButton filterButton;
    private FloatingActionButton addFloatButton;

    private RecyclerView recyclerView;
    private CategoryAdapter categoryAdapter;
    private List<Category> categoryList, filteredList;

    private final int[] selectedFilter = {0};

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

    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Split the input into words
        String[] words = input.split("\\s+");

        // Capitalize the first letter of each word
        StringBuilder capitalizedString = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                capitalizedString.append(word.substring(0, 1).toUpperCase())
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        // Remove the last space and return the result
        return capitalizedString.toString().trim();
    }

    private void searchCategory(String s){
        filteredList.clear();

        if (s.isEmpty()) {
            // If the search bar is empty, return the full category list
            filteredList.addAll(categoryList);
        } else {
            // Loop through your original list of categories
            for (Category category : categoryList) {
                // Check if the category title contains the query text (case-insensitive)
                if (category.getCategoryName().toLowerCase().contains(s.toLowerCase())) {
                    filteredList.add(category);  // Add it to the filtered list
                }
            }
        }
        categoryAdapter.notifyDataSetChanged();
    }

    private void updateCategoryRecyclerView(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences preferences1 = getSharedPreferences("user", MODE_PRIVATE);
        String user = preferences1.getString("uid", "");
        CollectionReference categoryRef = db.collection("category");

        Query.Direction orderDirection = null;
        if (selectedFilter[0] == 0){
            orderDirection = Query.Direction.ASCENDING;
        } else if (selectedFilter[0] == 1) {
            orderDirection = Query.Direction.DESCENDING;
        }

        categoryRef.whereEqualTo("userId", user)
                .orderBy("categoryTitle", orderDirection)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    categoryList.clear();

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String documentId = document.getId();
                        int categoryId = Integer.parseInt(documentId);
                        String title = document.getString("categoryTitle");

                        categoryList.add(new Category(categoryId, title));
                    }

                    categoryAdapter.notifyDataSetChanged();

                    String currentSearchText = searchBar.getQuery().toString();
                    searchCategory(currentSearchText);
                }
            }
        });
    }

    private void showFilterDialog() {
        String[] filterOptions = {"A to Z", "Z to A"};


        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(MPManageCategory.this);
        builder.setTitle("Select Filter");

        // Set up the single-choice items in the dialog
        builder.setSingleChoiceItems(filterOptions, selectedFilter[0], new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedFilter[0] = which;
                updateCategoryRecyclerView();
                dialog.dismiss();
            }
        });

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onCategoryUpdated() {
        updateCategoryRecyclerView();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.mp_manage_category);

        backButton = findViewById(R.id.back_button);
        searchBar = findViewById(R.id.search_bar);
        filterButton = findViewById(R.id.filter_button);
        addFloatButton = findViewById(R.id.add_float_button);

        recyclerView = findViewById(R.id.recycle_view);

        // Initialize RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize Item List
        categoryList = new ArrayList<>();
        filteredList = new ArrayList<>();

        // Initialize Adapter
        categoryAdapter = new CategoryAdapter(filteredList, MPManageCategory.this, this);
        recyclerView.setAdapter(categoryAdapter);

        updateCategoryRecyclerView();

        backButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        addFloatButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                View focusedView = getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(MPManageCategory.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }

                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MPManageCategory.this);
                View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet, null);
                bottomSheetDialog.setContentView(bottomSheetView);

                Button bottomSheetButton = bottomSheetView.findViewById(R.id.bottom_sheet_button);
                TextView bottomSheetTitle = bottomSheetView.findViewById(R.id.bottom_sheet_title);
                TextInputLayout bottomSheetInputLayout = bottomSheetView.findViewById(R.id.bottom_sheet_text_layout);
                TextInputEditText bottomSheetInput = bottomSheetView.findViewById(R.id.bottom_sheet_text_input);

                bottomSheetTitle.setText("Add New Category");
                bottomSheetInputLayout.setHint("Category Title");
                bottomSheetButton.setText("Done");

                bottomSheetButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ProgressDialog progressDialog = new ProgressDialog();
                        progressDialog.show(getSupportFragmentManager(), "progressDialog");
                        bottomSheetInput.clearFocus();

                        String newCategory = bottomSheetInput.getText().toString().trim();
                        if (newCategory.isEmpty()){
                            setLayoutError(bottomSheetInputLayout, "Required*");
                            progressDialog.dismiss();
                        } else {
                            SharedPreferences preferences1 = getSharedPreferences("user", MODE_PRIVATE);
                            String user = preferences1.getString("uid", "");

                            String capitalCategory = capitalizeFirstLetter(newCategory);

                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            CollectionReference categoryRef = db.collection("category");
                            categoryRef.whereEqualTo("userId", user)
                                    .whereEqualTo("categoryTitle", capitalCategory).get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful()) {
                                                if (!task.getResult().isEmpty()) {
                                                    setLayoutError(bottomSheetInputLayout, "Category already exists");
                                                    progressDialog.dismiss();
                                                } else {
                                                    setLayoutError(bottomSheetInputLayout, null);
                                                    DocumentReference counterRef = FirebaseFirestore.getInstance().collection("counter").document("category");
                                                    counterRef.update("current", FieldValue.increment(1))
                                                            .addOnSuccessListener(aVoid -> {
                                                                counterRef.get().addOnCompleteListener(counterTask -> {
                                                                    if (counterTask.isSuccessful()) {
                                                                        DocumentSnapshot document = counterTask.getResult();
                                                                        if (document.exists()) {
                                                                            Long newId = document.getLong("current");

                                                                            Map<String, Object> categoryData = new HashMap<>();
                                                                            categoryData.put("categoryTitle", capitalCategory);
                                                                            categoryData.put("userId", user);

                                                                            db.collection("category")
                                                                                    .document(String.valueOf(newId))
                                                                                    .set(categoryData)
                                                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                        @Override
                                                                                        public void onSuccess(Void unused) {
                                                                                            updateCategoryRecyclerView();
                                                                                            progressDialog.dismiss();
                                                                                            Toast.makeText(getApplicationContext(), "Successful Added", Toast.LENGTH_SHORT).show();
                                                                                            bottomSheetDialog.dismiss();
                                                                                        }
                                                                                    });
                                                                        }
                                                                    }
                                                                });
                                                            });
                                                }
                                            }
                                        }
                                    });
                        }
                    }
                });

                bottomSheetInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            setLayoutError(bottomSheetInputLayout, null);
                        }
                    }
                });

                bottomSheetDialog.show();
            }
        });

        filterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View focusedView = getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(MPManageCategory.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }
                showFilterDialog();
            }
        });

        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                View focusedView = getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(MPManageCategory.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                searchCategory(s);
                return false;
            }
        });
    }
}
