package com.example.billsphere;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class SRBills extends Fragment{

    private SearchView searchBar;
    private ImageButton filterButton;
    private RecyclerView recyclerView;

    private SRReceiptAdapter receiptAdapter;

    private List<SRReceipt> receiptList, filteredList;

    private final int[] selectedFilter = {0};

    private void showFilterDialog(){
        String[] paymentMethods = getResources().getStringArray(R.array.payment_methods);
        String[] filterOptions = new String[paymentMethods.length + 1];
        filterOptions[0] = "All Payment Methods"; // Set the first element
        System.arraycopy(paymentMethods, 0, filterOptions, 1, paymentMethods.length);

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Filter");

        // Set up the single-choice items in the dialog
        builder.setSingleChoiceItems(filterOptions, selectedFilter[0], new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedFilter[0] = which;
                updateReceiptRecyclerView();
                dialog.dismiss();
            }
        });

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateReceiptRecyclerView(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences preferences1 = getActivity().getSharedPreferences("user", Context.MODE_PRIVATE);
        String user = preferences1.getString("uid", "");
        CollectionReference receiptRef = db.collection("bill");

        Query query;
        if (selectedFilter[0] == 0){
            query = receiptRef.whereEqualTo("businessId", user)
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        } else {
            String payBy = null;
            String[] payMethods = getResources().getStringArray(R.array.payment_methods);
            if (selectedFilter[0] == 1) {
                payBy = payMethods[0];
            } else if (selectedFilter[0] == 2) {
                payBy = payMethods[1];
            } else if (selectedFilter[0] == 3) {
                payBy = payMethods[2];
            }
            query = receiptRef.whereEqualTo("businessId", user)
                    .whereEqualTo("paymentMethod", payBy)
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        }
        query.get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            receiptList.clear();
                            List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String receiptId = document.getId();
                                String customerId = document.getString("customerId");
                                String paymentMethod = document.getString("paymentMethod");

                                Timestamp timestamp = document.getTimestamp("timestamp");
                                LocalDateTime localDateTime = null;
                                if (timestamp != null) {
                                    localDateTime = timestamp.toDate()
                                            .toInstant()
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalDateTime();
                                }

                                double totalAmount = document.getDouble("totalAmount");

                                List<HashMap<String, Object>> productList = (List<HashMap<String, Object>>) document.get("selectedProducts");
                                ArrayList<CBProduct> selectedProducts = new ArrayList<>();
                                if (productList != null) {
                                    for (HashMap<String, Object> productData : productList) {
                                        String categoryId = (String) productData.get("categoryId");
                                        String productCode = (String) productData.get("productCode");
                                        String productId = (String) productData.get("productId");
                                        String productImage = (String) productData.get("productImage");
                                        String productName = (String) productData.get("productName");
                                        double productPrice = (double) productData.get("productPrice");
                                        double productProfit = (double) productData.get("productProfit");
                                        Object quantityObj = productData.get("quantity");
                                        int quantity = 0;
                                        if (quantityObj instanceof Long) {
                                            quantity = ((Long) quantityObj).intValue();
                                        } else if (quantityObj instanceof Integer) {
                                            quantity = (Integer) quantityObj;
                                        }

                                        // Create a CBProduct object and add it to the list
                                        CBProduct product = new CBProduct(productImage, productName, productPrice, categoryId,
                                                productId, productProfit, productCode);
                                        product.setQuantity(quantity);
                                        selectedProducts.add(product);
                                    }
                                }
                                SRReceipt temp = new SRReceipt(customerId, paymentMethod, totalAmount, localDateTime,
                                        selectedProducts, receiptId);
                                Task<DocumentSnapshot> userTask = db.collection("user").document(customerId).get();

                                userTask.addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        String name = userDoc.getString("userName");
                                        String email = userDoc.getString("userEmail");
                                        temp.setUserName(name);
                                        temp.setUserEmail(email);
                                    }
                                });
                                userTasks.add(userTask);
                                receiptList.add(temp);
                            }

                            Tasks.whenAllComplete(userTasks).addOnCompleteListener(taskList -> {
                                Collections.sort(receiptList, (r1, r2) -> {
                                    LocalDateTime time2 = r2.getTimestamp();
                                    LocalDateTime time1 = r1.getTimestamp();
                                    return time2.compareTo(time1); // Sort in descending order
                                });
                                receiptAdapter.notifyDataSetChanged();
                                String currentSearchText = searchBar.getQuery().toString();
                                searchReceipt(currentSearchText);
                            });
                        }
                    }
                });
    }

    private void searchReceipt(String s){
        filteredList.clear();

        if (s.isEmpty()) {
            filteredList.addAll(receiptList);
        } else {
            for (SRReceipt receipt : receiptList) {
                if (receipt.getReceiptId().toLowerCase().contains(s.toLowerCase())
                        ||receipt.getUserName().toLowerCase().contains(s.toLowerCase())
                        ||receipt.getUserEmail().toLowerCase().contains(s.toLowerCase())) {
                    filteredList.add(receipt);  // Add it to the filtered list
                }
            }
        }
        receiptAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();

        updateReceiptRecyclerView();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sr_bills, container, false);

        searchBar = view.findViewById(R.id.search_bar);
        filterButton = view.findViewById(R.id.filter_button);
        recyclerView = view.findViewById(R.id.recycle_view);

        receiptList = new ArrayList<>();
        filteredList = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        receiptAdapter = new SRReceiptAdapter(filteredList, this);
        recyclerView.setAdapter(receiptAdapter);

        updateReceiptRecyclerView();

        filterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View focusedView = getView().findFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
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
                View focusedView = getView().findFocus();
                if (focusedView != null){
                    focusedView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                searchReceipt(s);
                return false;
            }
        });

        return view;
    }
}
