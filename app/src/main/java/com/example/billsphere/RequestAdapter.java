package com.example.billsphere;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.ViewHolder>{
    private List<Request> requestList;
    private Fragment fragment;
    private RequestAdapter.OnRequestUpdateListener listener;

    public interface OnRequestUpdateListener {
        void OnRequestUpdated();
    }

    private void openPdf(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        fragment.getActivity().startActivity(intent);
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

                ContentResolver resolver = fragment.getActivity().getContentResolver();
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    try {
                        OutputStream outputStream = resolver.openOutputStream(uri);
                        if (outputStream != null) {
                            outputStream.write(bytes);
                            outputStream.close();
                            Toast.makeText(fragment.getContext(), "Download successful", Toast.LENGTH_SHORT).show();
                            openPdf(uri);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(fragment.getContext(), "Failed to save PDF", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    public RequestAdapter(List<Request> receiptList, Fragment fragment, OnRequestUpdateListener listener){
        this.requestList = receiptList;
        this.fragment = fragment;
        this.listener = listener;
    }

    private void uploadToRequest(Request request, String oldBusinessFileName, String oldOwnerFileName,
                                 String oldBusinessUrl, String oldOwnerUrl, String oldImageUrl,
                                 String address, String contact, String pos, String country, String dial,
                                 OnSuccessListener<Void> onSuccessListener){
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // Define paths for new storage locations
        StorageReference businessProofRef = storageRef.child("requestDocuments/" + request.getRequestId() + "/businessProof/" + oldBusinessFileName);
        StorageReference ownerProofRef = storageRef.child("requestDocuments/" + request.getRequestId() + "/ownerProof/" + oldOwnerFileName);
        StorageReference imageRef = storageRef.child("requestDocuments/" + request.getRequestId() + "/profileImage/" + request.getUserId());

        if (oldImageUrl != null) {
            // Download the existing documents from the old storage location and re-upload them to the new storage location
            uploadDocToNewPath(oldBusinessUrl, businessProofRef, url1 -> {
                uploadDocToNewPath(oldOwnerUrl, ownerProofRef, url2 -> {
                    uploadDocToNewPath(oldImageUrl, imageRef, url3 -> {
                        Map<String, Object> requestData = new HashMap<>();
                        requestData.put("businessProofUrl", url1);
                        requestData.put("ownerProofUrl", url2);
                        requestData.put("businessImage", url3);
                        requestData.put("businessName", request.getUserName());
                        requestData.put("businessEmail", request.getUserEmail());
                        requestData.put("businessAddress", address);
                        requestData.put("businessPosCode", pos);
                        requestData.put("businessCountry", country);
                        requestData.put("businessDialCode", dial);
                        requestData.put("businessContact", contact);
                        requestData.put("acceptRejectTime", FieldValue.serverTimestamp());

                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("request").document(request.getRequestId())
                                .update(requestData)  // This will add or update only the provided fields
                                .addOnSuccessListener(onSuccessListener);
                    });
                });
            });
        } else {
            uploadDocToNewPath(oldBusinessUrl, businessProofRef, url1 -> {
                uploadDocToNewPath(oldOwnerUrl, ownerProofRef, url2 -> {
                        Map<String, Object> requestData = new HashMap<>();
                        requestData.put("businessProofUrl", url1);
                        requestData.put("ownerProofUrl", url2);
                        requestData.put("businessName", request.getUserName());
                        requestData.put("businessEmail", request.getUserEmail());
                        requestData.put("businessAddress", address);
                        requestData.put("businessPosCode", pos);
                        requestData.put("businessCountry", country);
                        requestData.put("businessDialCode", dial);
                        requestData.put("businessContact", contact);
                        requestData.put("acceptRejectTime", FieldValue.serverTimestamp());

                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("request").document(request.getRequestId())
                                .update(requestData)  // This will add or update only the provided fields
                                .addOnSuccessListener(onSuccessListener);
                    });
                });
        }

    }

    private void uploadDocToNewPath(String oldFileUrl, StorageReference newRef, OnSuccessListener<String> onSuccessListener){
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference oldRef = storage.getReferenceFromUrl(oldFileUrl);

        // Download the file from old path and upload to new path
        oldRef.getBytes(Long.MAX_VALUE).addOnSuccessListener(bytes -> {
            newRef.putBytes(bytes).addOnSuccessListener(taskSnapshot -> {
                newRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Return the new URL after upload
                    onSuccessListener.onSuccess(uri.toString());
                });
            });
        });
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

    @NonNull
    @Override
    public RequestAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RequestAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.r_card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RequestAdapter.ViewHolder holder, int position) {
        Request request = requestList.get(position);

        holder.requestNumber.setText("Request "+request.getRequestId());
        holder.requestNumber.setPaintFlags(holder.requestNumber.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        LocalDateTime fullDateTime = request.getRequestTimestamp();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter datetimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String date = fullDateTime.format(dateFormatter);
        String time = fullDateTime.format(timeFormatter);
        holder.requestDate.setText(date);
        holder.requestTime.setText(time);

        if (request.getAcceptRejectTimestamp() == null){
            //Pending
            holder.acceptRejectDatetime.setVisibility(View.GONE);
            holder.acceptButton.setVisibility(View.VISIBLE);
            holder.rejectButton.setVisibility(View.VISIBLE);

            String userId = request.getUserId();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("user")
                    .document(userId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    String image = document.getString("userImage");
                                    String name = document.getString("userName");
                                    String address = document.getString("userAddress");
                                    String pos = document.getString("userPosCode");
                                    String country = document.getString("userCountry");
                                    String email = document.getString("userEmail");
                                    String dial = document.getString("userDialCode");
                                    String contact = document.getString("userContact");
                                    String businessDocPath = document.getString("businessProofUrl");
                                    String ownerDocPath = document.getString("ownerProofUrl");

                                    if (image != null){
                                        Glide.with(fragment).load(image)
                                                .apply(RequestOptions.circleCropTransform()).into(holder.businessImage);
                                    } else {
                                        holder.businessImage.setImageResource(R.drawable.null_profile_image);
                                    }
                                    holder.businessName.setText(name);
                                    holder.businessEmail.setText(email);
                                    String fullAddress = address + ", " + pos + ", " + country;
                                    holder.businessAddress.setText(fullAddress);
                                    String dialCode = dial.split(" ")[0];
                                    String fullContact = dialCode + " " + contact;
                                    holder.businessPhone.setText(fullContact);

                                    String businessWithoutParams = businessDocPath.split("\\?")[0];
                                    String businessDecodedUrl = Uri.decode(businessWithoutParams);
                                    String businessFileName = businessDecodedUrl.substring(businessDecodedUrl.lastIndexOf("/") + 1);
                                    holder.businessDocText.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            View focusedView = fragment.getActivity().getCurrentFocus();
                                            if (focusedView != null){
                                                focusedView.clearFocus();
                                                InputMethodManager imm = (InputMethodManager) fragment.requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                                if (imm != null) {
                                                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                                                }
                                            }
                                            downloadAndOpenPDF(businessDocPath, businessFileName);
                                        }
                                    });
                                    holder.businessDocButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            View focusedView = fragment.getActivity().getCurrentFocus();
                                            if (focusedView != null){
                                                focusedView.clearFocus();
                                                InputMethodManager imm = (InputMethodManager) fragment.requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                                if (imm != null) {
                                                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                                                }
                                            }
                                            downloadAndOpenPDF(businessDocPath, businessFileName);
                                        }
                                    });

                                    String ownerWithoutParams = ownerDocPath.split("\\?")[0];
                                    String ownerDecodedUrl = Uri.decode(ownerWithoutParams);
                                    String ownerFileName = ownerDecodedUrl.substring(ownerDecodedUrl.lastIndexOf("/") + 1);
                                    holder.ownerDocText.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            View focusedView = fragment.getActivity().getCurrentFocus();
                                            if (focusedView != null){
                                                focusedView.clearFocus();
                                                InputMethodManager imm = (InputMethodManager) fragment.requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                                if (imm != null) {
                                                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                                                }
                                            }
                                            downloadAndOpenPDF(ownerDocPath, ownerFileName);
                                        }
                                    });
                                    holder.ownerDocButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            View focusedView = fragment.getActivity().getCurrentFocus();
                                            if (focusedView != null){
                                                focusedView.clearFocus();
                                                InputMethodManager imm = (InputMethodManager) fragment.requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                                if (imm != null) {
                                                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                                                }
                                            }
                                            downloadAndOpenPDF(ownerDocPath, ownerFileName);
                                        }
                                    });

                                    holder.acceptButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            View focusedView = fragment.getActivity().getCurrentFocus();
                                            if (focusedView != null) {
                                                focusedView.clearFocus();
                                                InputMethodManager imm = (InputMethodManager) fragment.requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                                if (imm != null) {
                                                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                                                }
                                            }
                                            ProgressDialog progressDialog = new ProgressDialog();
                                            progressDialog.show(fragment.getActivity().getSupportFragmentManager(), "progressDialog");

                                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                                            db.collection("user").document(request.getUserId())
                                                    .update("userBusinessStatus", "accepted")
                                                    .addOnSuccessListener(aVoid -> {
                                                        uploadToRequest(request, businessFileName, ownerFileName, businessDocPath, ownerDocPath,
                                                                image, address, contact, pos, country, dial, aVoid1 -> {
                                                                    listener.OnRequestUpdated();
                                                                    progressDialog.dismiss();
                                                                    Toast.makeText(fragment.getContext(), "Request accepted", Toast.LENGTH_SHORT).show();
                                                                });
                                                    });
                                        }
                                    });

                                    holder.rejectButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            View focusedView = fragment.getActivity().getCurrentFocus();
                                            if (focusedView != null) {
                                                focusedView.clearFocus();
                                                InputMethodManager imm = (InputMethodManager) fragment.requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                                if (imm != null) {
                                                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                                                }
                                            }
                                            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(fragment.getContext());
                                            View bottomSheetView = fragment.getActivity().getLayoutInflater().inflate(R.layout.bottom_sheet, null);
                                            bottomSheetDialog.setContentView(bottomSheetView);

                                            Button bottomSheetButton = bottomSheetView.findViewById(R.id.bottom_sheet_button);
                                            TextView bottomSheetTitle = bottomSheetView.findViewById(R.id.bottom_sheet_title);
                                            TextInputLayout bottomSheetInputLayout = bottomSheetView.findViewById(R.id.bottom_sheet_text_layout);
                                            TextInputEditText bottomSheetInput = bottomSheetView.findViewById(R.id.bottom_sheet_text_input);

                                            bottomSheetTitle.setText("Reject " + name);
                                            bottomSheetInputLayout.setHint("Reason");
                                            bottomSheetButton.setText("Done");

                                            bottomSheetInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                                                @Override
                                                public void onFocusChange(View v, boolean hasFocus) {
                                                    if (hasFocus) {
                                                        setLayoutError(bottomSheetInputLayout, null);
                                                    }
                                                }
                                            });

                                            bottomSheetButton.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    ProgressDialog progressDialog = new ProgressDialog();
                                                    progressDialog.show(fragment.getActivity().getSupportFragmentManager(), "progressDialog");

                                                    String reason =bottomSheetInput.getText().toString().trim();
                                                    if (!reason.isEmpty()){
                                                        setLayoutError(bottomSheetInputLayout, null);
                                                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                                                        db.collection("user").document(request.getUserId())
                                                                .update("userBusinessStatus", "rejected")
                                                                .addOnSuccessListener(aVoid -> {
                                                                    db.collection("request").document(request.getRequestId())
                                                                                    .update("rejectReason", reason)
                                                                                            .addOnSuccessListener(aVoid2 -> {
                                                                                                uploadToRequest(request, businessFileName, ownerFileName, businessDocPath, ownerDocPath,
                                                                                                        image, address, contact, pos, country, dial, aVoid1 -> {
                                                                                                            listener.OnRequestUpdated();
                                                                                                            bottomSheetDialog.dismiss();
                                                                                                            progressDialog.dismiss();
                                                                                                            Toast.makeText(fragment.getContext(), "Request rejected", Toast.LENGTH_SHORT).show();
                                                                                                        });
                                                                                            });
                                                                });
                                                    } else {
                                                        setLayoutError(bottomSheetInputLayout, "Required*");
                                                        progressDialog.dismiss();
                                                    }
                                                }
                                            });
                                            bottomSheetDialog.show();
                                        }
                                    });
                                }
                            }
                        }
                    });
        } else {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("request")
                    .document(request.getRequestId())
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    String image = document.getString("businessImage");
                                    String name = document.getString("businessName");
                                    String address = document.getString("businessAddress");
                                    String pos = document.getString("businessPosCode");
                                    String country = document.getString("businessCountry");
                                    String email = document.getString("businessEmail");
                                    String dial = document.getString("businessDialCode");
                                    String contact = document.getString("businessContact");
                                    String businessDocPath = document.getString("businessProofUrl");
                                    String ownerDocPath = document.getString("ownerProofUrl");

                                    if (image != null) {
                                        Glide.with(fragment).load(image)
                                                .apply(RequestOptions.circleCropTransform()).into(holder.businessImage);
                                    } else {
                                        holder.businessImage.setImageResource(R.drawable.null_profile_image);
                                    }
                                    holder.businessName.setText(name);
                                    holder.businessEmail.setText(email);
                                    String fullAddress = address + ", " + pos + ", " + country;
                                    holder.businessAddress.setText(fullAddress);
                                    String dialCode = dial.split(" ")[0];
                                    String fullContact = dialCode + " " + contact;
                                    holder.businessPhone.setText(fullContact);

                                    String businessWithoutParams = businessDocPath.split("\\?")[0];
                                    String businessDecodedUrl = Uri.decode(businessWithoutParams);
                                    String businessFileName = businessDecodedUrl.substring(businessDecodedUrl.lastIndexOf("/") + 1);
                                    holder.businessDocText.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            View focusedView = fragment.getActivity().getCurrentFocus();
                                            if (focusedView != null) {
                                                focusedView.clearFocus();
                                                InputMethodManager imm = (InputMethodManager) fragment.requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                                if (imm != null) {
                                                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                                                }
                                            }
                                            downloadAndOpenPDF(businessDocPath, businessFileName);
                                        }
                                    });
                                    holder.businessDocButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            View focusedView = fragment.getActivity().getCurrentFocus();
                                            if (focusedView != null) {
                                                focusedView.clearFocus();
                                                InputMethodManager imm = (InputMethodManager) fragment.requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                                if (imm != null) {
                                                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                                                }
                                            }
                                            downloadAndOpenPDF(businessDocPath, businessFileName);
                                        }
                                    });

                                    String ownerWithoutParams = ownerDocPath.split("\\?")[0];
                                    String ownerDecodedUrl = Uri.decode(ownerWithoutParams);
                                    String ownerFileName = ownerDecodedUrl.substring(ownerDecodedUrl.lastIndexOf("/") + 1);
                                    holder.ownerDocText.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            View focusedView = fragment.getActivity().getCurrentFocus();
                                            if (focusedView != null) {
                                                focusedView.clearFocus();
                                                InputMethodManager imm = (InputMethodManager) fragment.requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                                if (imm != null) {
                                                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                                                }
                                            }
                                            downloadAndOpenPDF(ownerDocPath, ownerFileName);
                                        }
                                    });
                                    holder.ownerDocButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            View focusedView = fragment.getActivity().getCurrentFocus();
                                            if (focusedView != null) {
                                                focusedView.clearFocus();
                                                InputMethodManager imm = (InputMethodManager) fragment.requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                                if (imm != null) {
                                                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                                                }
                                            }
                                            downloadAndOpenPDF(ownerDocPath, ownerFileName);
                                        }
                                    });
                                }
                            }
                        }
                    });

            if (request.getRejectReason() == null){
                // Accepted
                holder.acceptButton.setVisibility(View.GONE);
                holder.rejectButton.setVisibility(View.GONE);
                holder.acceptRejectDatetime.setVisibility(View.VISIBLE);
                holder.acceptRejectDatetime.setClickable(false);
                holder.acceptRejectDatetime.setFocusable(false);
                holder.acceptRejectDatetime.setTypeface(null, Typeface.NORMAL);

                LocalDateTime acceptDateTime = request.getAcceptRejectTimestamp();
                String acceptDatetime = acceptDateTime.format(datetimeFormatter);
                holder.acceptRejectDatetime.setText("Accepted on\n"+acceptDatetime);

            } else {
                // Rejected
                holder.acceptButton.setVisibility(View.GONE);
                holder.rejectButton.setVisibility(View.GONE);
                holder.acceptRejectDatetime.setVisibility(View.VISIBLE);
                holder.acceptRejectDatetime.setClickable(true);
                holder.acceptRejectDatetime.setFocusable(true);
                holder.acceptRejectDatetime.setTypeface(null, Typeface.BOLD);

                LocalDateTime rejectDateTime = request.getAcceptRejectTimestamp();
                String rejectDatetime = rejectDateTime.format(datetimeFormatter);
                holder.acceptRejectDatetime.setText("Rejected on\n"+rejectDatetime);

                holder.acceptRejectDatetime.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(fragment.getContext());
                        View bottomSheetView = fragment.getActivity().getLayoutInflater().inflate(R.layout.bottom_sheet_reject_reason, null);
                        bottomSheetDialog.setContentView(bottomSheetView);
                        TextView bottomSheetTitle = bottomSheetView.findViewById(R.id.bottom_sheet_title);
                        TextView bottomSheetInput = bottomSheetView.findViewById(R.id.bottom_sheet_text_input);
                        bottomSheetTitle.setText("Reject Reason");
                        bottomSheetInput.setText(request.getRejectReason());
                        bottomSheetDialog.show();
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView businessImage;
        private TextView requestDate, requestTime, requestNumber;
        private TextView businessName, businessEmail, businessPhone, businessAddress;
        private TextView businessDocText, ownerDocText;
        private ImageButton businessDocButton, ownerDocButton;
        private TextView acceptRejectDatetime;
        private Button acceptButton, rejectButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            businessImage = itemView.findViewById(R.id.business_image);
            requestDate = itemView.findViewById(R.id.request_date);
            requestTime = itemView.findViewById(R.id.request_time);
            requestNumber = itemView.findViewById(R.id.request_number);
            businessName = itemView.findViewById(R.id.business_name);
            businessEmail = itemView.findViewById(R.id.business_email);
            businessPhone = itemView.findViewById(R.id.business_phone);
            businessAddress = itemView.findViewById(R.id.business_address);
            businessDocText = itemView.findViewById(R.id.business_doc_text);
            businessDocButton = itemView.findViewById(R.id.business_doc_button);
            ownerDocText = itemView.findViewById(R.id.owner_doc_text);
            ownerDocButton = itemView.findViewById(R.id.owner_doc_button);
            acceptRejectDatetime = itemView.findViewById(R.id.acceptRejectDatetime);
            acceptButton = itemView.findViewById(R.id.accept_button);
            rejectButton = itemView.findViewById(R.id.reject_button);
        }
    }
}
