package com.example.billsphere;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.SearchView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class F1SellersAdmin extends Fragment {

    private SearchView searchBar;
    private ImageButton qrScannerButton;
    private RecyclerView recyclerView;

    private SSellerAdminAdapter sellerAdapter;

    private List<SSeller> sellerList, filteredList;

    private void searchSeller(String s){
        filteredList.clear();

        if (s.isEmpty()) {
            filteredList.addAll(sellerList);
        } else {
            for (SSeller seller : sellerList) {
                if (seller.getBusinessName().toLowerCase().contains(s.toLowerCase()) ||
                        seller.getBusinessEmail().toLowerCase().contains(s.toLowerCase())) {
                    filteredList.add(seller);  // Add it to the filtered list
                }
            }
        }
        sellerAdapter.notifyDataSetChanged();
    }

    private void scanSellerCode(){
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan a Seller QR Code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setCaptureActivity(CaptureAct.class);
        QrCodeScanLauncher.launch(options);
    }

    private final ActivityResultLauncher<ScanOptions> QrCodeScanLauncher = registerForActivityResult(new ScanContract(), result->{
        if(result.getContents() != null){
            String scannedEmail = result.getContents();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("user")
                    .whereEqualTo("userEmail", scannedEmail)
                    .whereEqualTo("userType", "business")
                    .whereEqualTo("userBusinessStatus", "accepted")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                if (!task.getResult().isEmpty()) {
                                    searchBar.setQuery(scannedEmail, true);
                                } else {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                    builder.setTitle("Error");
                                    builder.setMessage("Invalid QR Code");
                                    builder.setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.dismiss();
                                            scanSellerCode();
                                        }
                                    });
                                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.dismiss();
                                        }
                                    });
                                    builder.show();
                                }
                            }
                        }
                    });
        }
    });

    private void updateSellerRecyclerView(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference businessRef = db.collection("user");

        businessRef.whereEqualTo("userType", "business")
                .whereEqualTo("userBusinessStatus", "accepted")
                .orderBy("userName", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            sellerList.clear();

                            for (DocumentSnapshot document : task.getResult()) {
                                String documentId = document.getId();
                                String businessName = document.getString("userName");
                                String businessEmail = document.getString("userEmail");
                                String businessImage = document.getString("userImage");
                                String businessAddress = document.getString("userAddress");
                                String businessContact = document.getString("userContact");
                                String businessCountry = document.getString("userCountry");
                                String businessDial = document.getString("userDialCode");
                                String businessPos = document.getString("userPosCode");

                                sellerList.add(new SSeller(businessImage, businessName, businessEmail, businessContact,
                                        businessDial, businessAddress, businessPos, businessCountry, documentId));
                            }

                            sellerAdapter.notifyDataSetChanged();

                            String currentSearchText = searchBar.getQuery().toString();
                            searchSeller(currentSearchText);
                        }
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();

        updateSellerRecyclerView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment1_sellers, container, false);

        searchBar = view.findViewById(R.id.search_bar);
        qrScannerButton = view.findViewById(R.id.qr_scanner_button);
        recyclerView = view.findViewById(R.id.recycle_view);

        sellerList = new ArrayList<>();
        filteredList = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        sellerAdapter = new SSellerAdminAdapter(filteredList, this);
        recyclerView.setAdapter(sellerAdapter);

        updateSellerRecyclerView();

        qrScannerButton.setOnClickListener(new View.OnClickListener() {
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
                scanSellerCode();
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
                searchSeller(s);
                return false;
            }
        });
        return view;
    }
}
