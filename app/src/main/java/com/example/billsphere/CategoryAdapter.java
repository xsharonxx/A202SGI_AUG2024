package com.example.billsphere;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private List<Category> categoryList;
    private Context context;
    private OnCategoryUpdateListener listener;

    public interface OnCategoryUpdateListener {
        void onCategoryUpdated();
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

    public CategoryAdapter(List<Category> categoryList, Context context, OnCategoryUpdateListener listener){
        this.categoryList = categoryList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.mp_category, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.categoryName.setText(category.getCategoryName());

        holder.editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View focusedView = ((Activity) context).getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
                View bottomSheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet, null);
                bottomSheetDialog.setContentView(bottomSheetView);

                Button bottomSheetButton = bottomSheetView.findViewById(R.id.bottom_sheet_button);
                TextView bottomSheetTitle = bottomSheetView.findViewById(R.id.bottom_sheet_title);
                TextInputLayout bottomSheetInputLayout = bottomSheetView.findViewById(R.id.bottom_sheet_text_layout);
                TextInputEditText bottomSheetInput = bottomSheetView.findViewById(R.id.bottom_sheet_text_input);

                bottomSheetTitle.setText("Edit Category");
                bottomSheetInputLayout.setHint("Category Title");
                String currentCategory = category.getCategoryName();
                bottomSheetInput.setText(currentCategory);
                bottomSheetButton.setText("Done");

                bottomSheetButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ProgressDialog progressDialog = new ProgressDialog();
                        progressDialog.show(((AppCompatActivity) context).getSupportFragmentManager(), "progressDialog");
                        bottomSheetInput.clearFocus();

                        String newCategory = bottomSheetInput.getText().toString().trim();
                        if (newCategory.isEmpty()){
                            setLayoutError(bottomSheetInputLayout, "Required*");
                            progressDialog.dismiss();
                        } else {
                            SharedPreferences preferences1 = context.getSharedPreferences("user", Context.MODE_PRIVATE);
                            String user = preferences1.getString("uid", "");

                            String capitalCategory = capitalizeFirstLetter(newCategory);

                            if (capitalCategory.equals(currentCategory)){
                                progressDialog.dismiss();
                                Toast.makeText(context, "Identical Category. No Changes.", Toast.LENGTH_SHORT).show();
                                bottomSheetDialog.dismiss();
                            } else {
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
                                                    } else {
                                                        setLayoutError(bottomSheetInputLayout, null);
                                                        int documentId = category.getCategoryId();
                                                        DocumentReference categoryDocRef = db.collection("category").document(String.valueOf(documentId));
                                                        Map<String, Object> updatedCategory = new HashMap<>();
                                                        updatedCategory.put("categoryTitle", capitalCategory);
                                                        categoryDocRef.update(updatedCategory)
                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void aVoid) {
                                                                        listener.onCategoryUpdated();
                                                                        progressDialog.dismiss();
                                                                        Toast.makeText(context, "Successful Edited", Toast.LENGTH_SHORT).show();
                                                                        bottomSheetDialog.dismiss();
                                                                    }
                                                                });
                                                    }
                                                }
                                            }
                                        });
                            }
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

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View focusedView = ((Activity) context).getCurrentFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }

                new AlertDialog.Builder(context)
                        .setTitle("Confirm Deletion")
                        .setMessage("Are you sure you want to delete " + category.getCategoryName() + " category?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                db.collection("product")
                                        .whereEqualTo("categoryId", String.valueOf(category.getCategoryId()))
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                                    new AlertDialog.Builder(context)
                                                            .setTitle("Error")
                                                            .setMessage("This " + category.getCategoryName() + " category cannot be deleted as it has products associated with it.")
                                                            .setPositiveButton("OK", null)
                                                            .show();// Products exist
                                                } else {
                                                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                                                    db.collection("category")
                                                            .document(String.valueOf(category.getCategoryId()))
                                                            .delete()
                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()) {
                                                                        listener.onCategoryUpdated();
                                                                        Toast.makeText(context, "Successful deleted", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                }
                                                            });
                                                }
                                            }
                                        });
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView categoryName;
        ImageButton editButton, deleteButton;

        public ViewHolder(@NonNull View itemView){
            super(itemView);

            categoryName = itemView.findViewById(R.id.category_name);
            editButton = itemView.findViewById(R.id.edit_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}
