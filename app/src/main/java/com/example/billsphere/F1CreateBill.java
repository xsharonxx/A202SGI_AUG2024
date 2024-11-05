package com.example.billsphere;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class F1CreateBill extends Fragment {

    private SearchView searchBar;
    private ImageButton qrScannerButton;
    private RecyclerView recyclerView;

    private TextView mainText, secondText, thirdText;
    private Button goToProfileButton;

    private CBCustomerAdapter customerAdapter;
    private List<CBCustomer> customerList, filteredList;

    private void searchCustomer(String s){
        filteredList.clear();

        if (s.isEmpty()) {
            filteredList.addAll(customerList);
        } else {
            for (CBCustomer customer : customerList) {
                if (customer.getCustomerName().toLowerCase().contains(s.toLowerCase()) ||
                        customer.getCustomerEmail().toLowerCase().contains(s.toLowerCase())) {
                    filteredList.add(customer);  // Add it to the filtered list
                }
            }
        }
        customerAdapter.notifyDataSetChanged();
    }

    private void updateCustomerRecyclerView(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference customerRef = db.collection("user");

        customerRef.whereEqualTo("userType", "customer")
                .orderBy("userName", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            customerList.clear();

                            for (DocumentSnapshot document : task.getResult()) {
                                String documentId = document.getId();
                                String customerName = document.getString("userName");
                                String customerEmail = document.getString("userEmail");
                                String customerImage = document.getString("userImage");
                                String customerAddress = document.getString("userAddress");
                                String customerContact = document.getString("userContact");
                                String customerCountry = document.getString("userCountry");
                                String customerDial = document.getString("userDialCode");
                                String customerPos = document.getString("userPosCode");

                                customerList.add(new CBCustomer(customerImage, customerName, customerEmail, customerContact,
                                        customerDial, customerAddress, customerPos, customerCountry, documentId));
                            }

                            customerAdapter.notifyDataSetChanged();

                            String currentSearchText = searchBar.getQuery().toString();
                            searchCustomer(currentSearchText);
                        }
                    }
                });
        }

    private void scanCustomerCode(){
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan a Customer QR Code");
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
                   .whereEqualTo("userType", "customer")
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
                                           scanCustomerCode();
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

    private void updatePendingRejected(String status){
        if (status.equals("rejected")){
            mainText.setText(R.string.request_rejected);
            secondText.setText(R.string.check_right_resubmit);
            thirdText.setText(R.string.reject_reason);
            thirdText.setFocusable(true);
            thirdText.setClickable(true);
            thirdText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    SharedPreferences preferences1 = getActivity().getSharedPreferences("user", Context.MODE_PRIVATE);
                    String user = preferences1.getString("uid", "");
                    db.collection("request")
                            .whereEqualTo("userId", user)
                            .orderBy("acceptRejectTime", Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                    DocumentSnapshot latestRequest = task.getResult().getDocuments().get(0);
                                    String rejectReason = latestRequest.getString("rejectReason");
                                    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
                                    View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_reject_reason, null);
                                    bottomSheetDialog.setContentView(bottomSheetView);
                                    TextView bottomSheetTitle = bottomSheetView.findViewById(R.id.bottom_sheet_title);
                                    TextView bottomSheetInput = bottomSheetView.findViewById(R.id.bottom_sheet_text_input);
                                    bottomSheetTitle.setText("Reject Reason");
                                    bottomSheetInput.setText(rejectReason);
                                    bottomSheetDialog.show();
                                }
                            });
                }
            });
        } else if (status.equals("pending")){
            mainText.setText(R.string.request_in_progress);
            secondText.setText(R.string.take3_5business_day);
            thirdText.setText(R.string.more_details);
            thirdText.setFocusable(false);
            thirdText.setClickable(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences preferences = getActivity().getSharedPreferences("businessStatus", Context.MODE_PRIVATE);
        String status = preferences.getString("status", "");
        if (status.equals("accepted")) {
            updateCustomerRecyclerView();
        } else {
            updatePendingRejected(status);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view;

        SharedPreferences preferences = getActivity().getSharedPreferences("businessStatus", Context.MODE_PRIVATE);
        String status = preferences.getString("status", "");

        if (status.equals("accepted")) {
            view = inflater.inflate(R.layout.fragment1_create_bill, container, false);

            searchBar = view.findViewById(R.id.search_bar);
            qrScannerButton = view.findViewById(R.id.qr_scanner_button);
            recyclerView = view.findViewById(R.id.recycle_view);

            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

            customerList = new ArrayList<>();
            filteredList = new ArrayList<>();

            customerAdapter = new CBCustomerAdapter(filteredList, this);
            recyclerView.setAdapter(customerAdapter);

            updateCustomerRecyclerView();

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
                    scanCustomerCode();
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
                    searchCustomer(s);
                    return false;
                }
            });
        } else {
            view = inflater.inflate(R.layout.fragment_pending_reject, container, false);

            mainText = view.findViewById(R.id.main_text);
            secondText = view.findViewById(R.id.second_text);
            thirdText = view.findViewById(R.id.third_text);
            goToProfileButton = view.findViewById(R.id.go_to_profile_button);

            updatePendingRejected(status);

            goToProfileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), ProfileBusiness.class);
                    startActivity(intent);
                }
            });

        }
        return view;
    }
}
