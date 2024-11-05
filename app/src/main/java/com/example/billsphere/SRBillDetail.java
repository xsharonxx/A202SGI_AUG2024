package com.example.billsphere;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class SRBillDetail extends AppCompatActivity {

    private String receiptId;

    private TextView overallTotal, billDatetime, profitAmount, billMethod;

    private ImageButton backButton;
    private TextView receiptTitle;
    private ImageView customerImage;
    private TextView customerName, customerEmail, customerPhone, customerAddress;
    private RecyclerView recyclerView;
    private Button eReceiptButton;

    private SRDetailAdapter detailAdapter;
    private ArrayList<CBProduct> selectedProducts;

    private void generateReceipt(String businessName, String businessAddress, String businessContact, String businessEmail,
                                 String customerName, String customerEmail, ArrayList<CBProduct> selectedProducts, String receiptDateTime,
                                 ProgressDialog progressDialog){
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size page

        Paint paint = new Paint();
        paint.setTextSize(14);
        paint.setColor(Color.BLACK);

        Paint boldPaint = new Paint();
        boldPaint.setTextSize(20);
        boldPaint.setColor(Color.BLACK);
        boldPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint smallBoldPaint = new Paint();
        smallBoldPaint.setTextSize(16);
        smallBoldPaint.setColor(Color.BLACK);
        smallBoldPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        int pageWidth = pageInfo.getPageWidth();

        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        float businessNameX = (pageWidth - boldPaint.measureText(businessName)) / 2;
        float businessEmailX = (pageWidth - paint.measureText(businessEmail)) / 2;
        float businessAddressX = (pageWidth - paint.measureText(businessAddress)) / 2;
        float businessPhoneX = (pageWidth - paint.measureText(businessContact)) / 2;

        canvas.drawText(businessName, businessNameX, 100, boldPaint);
        canvas.drawText(businessEmail, businessEmailX, 120, paint);
        canvas.drawText(businessAddress, businessAddressX, 140, paint);
        canvas.drawText(businessContact, businessPhoneX, 160, paint);

        canvas.drawLine(50, 200, 545, 200, paint);

        String receipt = "Receipt " + receiptId;
        canvas.drawText(receipt, 50, 240, boldPaint);

        float dateTimeWidth = paint.measureText(receiptDateTime);
        float dateTimeX = pageWidth - dateTimeWidth - 50;
        canvas.drawText(receiptDateTime, dateTimeX, 240, paint);

        canvas.drawText(customerName, 50, 270, smallBoldPaint);
        canvas.drawText(customerEmail, 50, 290, paint);

        canvas.drawLine(50, 320, 545, 320, paint);

        int startX = 50;
        int startY = 360;

        int pageHeight = pageInfo.getPageHeight();
        int maxYPosition = pageHeight - 100;
        int lineHeight = 10;
        int interLineHeight = 20;
        int numColumnWidth = 35;
        int productCodeColumnWidth = 110;
        int productNameColumnWidth = 140;
        int unitPriceColumnWidth = 80;
        int quantityColumnWidth = 50;
        int priceColumnWidth = 80;

        canvas.drawText("No", startX, startY, smallBoldPaint);
        canvas.drawText("Code", startX+35, startY, smallBoldPaint);
        canvas.drawText("Name", startX+145, startY, smallBoldPaint);
        canvas.drawText("Per", startX+285, startY, smallBoldPaint);
        canvas.drawText("Qty", startX+365, startY, smallBoldPaint);
        canvas.drawText("Price", startX+415, startY, smallBoldPaint);

        startY += 30;
        canvas.drawLine(50, startY, 545, startY, paint);
        startY += 40;

        int count = 0;
        double finalTotal = 0;
        for (CBProduct product : selectedProducts){
            count = count + 1;
            String countString = String.valueOf(count);
            String productCode = product.getProductCode();
            String productName = product.getProductName();
            int quantity = product.getQuantity();
            String quantityString = String.valueOf(quantity);
            double price = product.getProductPrice();
            Locale locale = new Locale("ms", "MY");
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
            String formattedPrice = currencyFormatter.format(price);
            double itemTotalPrice = quantity * price;
            String formattedItemTotal = currencyFormatter.format(itemTotalPrice);
            finalTotal = itemTotalPrice + finalTotal;

            List<String> countLines = splitNumberToFit(countString, paint, numColumnWidth);
            List<String> productCodeLines = splitNumberToFit(productCode, paint, productCodeColumnWidth);
            List<String> productNameLines = splitTextToFit(productName, paint, productNameColumnWidth);
            List<String> unitPriceLines = splitNumberToFit(formattedPrice, paint, unitPriceColumnWidth);
            List<String> quantityLines = splitNumberToFit(quantityString, paint, quantityColumnWidth);
            List<String> priceLines = splitNumberToFit(formattedItemTotal, paint, priceColumnWidth);

            int maxLines = Math.max(countLines.size(), Math.max(productCodeLines.size(),
                    Math.max(productNameLines.size(),
                            Math.max(unitPriceLines.size(), priceLines.size()))));

            for (int i = 0; i < maxLines; i++) {
                if (startY > maxYPosition) {
                    // Finish the current page and start a new one if necessary
                    document.finishPage(page);
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    startY = 100; // Reset Y position for the new page
                }

                // Draw each line, if available
                String countLine = (i < countLines.size()) ? countLines.get(i) : "";
                String codeLine = (i < productCodeLines.size()) ? productCodeLines.get(i) : "";
                String nameLine = (i < productNameLines.size()) ? productNameLines.get(i) : "";
                String unitPriceLine = (i < unitPriceLines.size()) ? unitPriceLines.get(i) : "";
                String quantityLine = (i < quantityLines.size()) ? quantityLines.get(i) : "";
                String priceLine = (i < priceLines.size()) ? priceLines.get(i) : "";

                // Draw the text at specified positions
                canvas.drawText(countLine, startX, startY, paint);
                canvas.drawText(codeLine, startX+35, startY, paint);
                canvas.drawText(nameLine, startX+145, startY, paint);
                canvas.drawText(unitPriceLine, startX+285, startY, paint);
                canvas.drawText(quantityLine, startX+365, startY, paint);
                canvas.drawText(priceLine, startX+415, startY, paint);

                startY += interLineHeight; // Move down for the next line
            }
            startY += lineHeight;
        }

        if (startY > maxYPosition) {
            document.finishPage(page);
            page = document.startPage(pageInfo);
            canvas = page.getCanvas();
            startY = 100;
        }

        canvas.drawLine(50, startY, 545, startY, paint);
        startY += 40;

        if (startY > maxYPosition) {
            document.finishPage(page);
            page = document.startPage(pageInfo);
            canvas = page.getCanvas();
            startY = 100;
        }

        Locale locale = new Locale("ms", "MY");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
        String formattedTotal = currencyFormatter.format(finalTotal);
        String totalText = "Total: " + formattedTotal;

        float totalWidth = boldPaint.measureText(totalText);
        float xPosition = pageWidth - totalWidth - 50;

        canvas.drawText(totalText, xPosition, startY, boldPaint);
        String payByText = billMethod.getText().toString().trim();
        canvas.drawText(payByText, startX, startY, smallBoldPaint);

        document.finishPage(page);

        downloadReceipt(document, progressDialog);
        document.close();
    }

    private List<String> splitNumberToFit(String text, Paint paint, int maxWidth){
        List<String> lines = new ArrayList<>();
        StringBuilder currentPart = new StringBuilder();

        for (char c : text.toCharArray()) {
            if (paint.measureText(currentPart.toString() + c) > maxWidth) {
                if (currentPart.length() > 0) {
                    lines.add(currentPart.toString().trim());
                }
                currentPart = new StringBuilder();
                currentPart.append(c);
            } else {
                currentPart.append(c);
            }
        }
        if (!currentPart.toString().isEmpty()) {
            lines.add(currentPart.toString().trim());
        }
        return lines;
    }

    private List<String> splitTextToFit(String text, Paint paint, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (paint.measureText(currentLine + word) < maxWidth) {
                currentLine.append(word).append(" ");
            } else {
                lines.add(currentLine.toString().trim());
                currentLine = new StringBuilder(word + " ");
            }
        }
        // Add the last line
        if (!currentLine.toString().isEmpty()) {
            lines.add(currentLine.toString().trim());
        }
        return lines;
    }

    private void downloadReceipt(PdfDocument document, ProgressDialog progressDialog){
        ContentValues values = new ContentValues();
        String receiptName = "e-receipt " + receiptId + ".pdf";
        values.put(MediaStore.Downloads.DISPLAY_NAME, receiptName);
        values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS); // Save to Downloads directory

        ContentResolver resolver = getContentResolver();
        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

        if (uri != null) {
            try {
                OutputStream outputStream = resolver.openOutputStream(uri);
                if (outputStream != null) {
                    document.writeTo(outputStream);
                    outputStream.close();
                    progressDialog.dismiss();
                    Toast.makeText(this, "Successful Download E-Receipt", Toast.LENGTH_SHORT).show();
                    openPdf(uri);
                }
            } catch (IOException e) {
                e.printStackTrace();
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to save receipt!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openPdf(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.sr_bill_detail);

        Intent intent = getIntent();
        receiptId = intent.getStringExtra("RECEIPT_ID");

        overallTotal  = findViewById(R.id.overall_total);
        billDatetime = findViewById(R.id.bill_datetime);
        profitAmount = findViewById(R.id.profit_amount);
        billMethod = findViewById(R.id.bill_method);

        backButton = findViewById(R.id.back_button);
        receiptTitle = findViewById(R.id.receipt_title);
        customerImage = findViewById(R.id.customer_image);
        customerName = findViewById(R.id.customer_name);
        customerEmail = findViewById(R.id.customer_email);
        customerPhone = findViewById(R.id.customer_phone);
        customerAddress = findViewById(R.id.customer_address);
        recyclerView = findViewById(R.id.recycle_view);
        eReceiptButton = findViewById(R.id.e_receipt_button);

        selectedProducts = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        detailAdapter = new SRDetailAdapter(selectedProducts, this);
        recyclerView.setAdapter(detailAdapter);

        receiptTitle.setText("Receipt " + receiptId);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("bill")
                .document(receiptId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                           @Override
                                           public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                               if (task.isSuccessful()) {
                                                   DocumentSnapshot document = task.getResult();
                                                   if (document.exists()) {
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

                                                       Locale locale = new Locale("ms", "MY");
                                                       NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
                                                       String formattedTotal = currencyFormatter.format(totalAmount);
                                                       overallTotal.setText(formattedTotal);

                                                       DateTimeFormatter datetimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                                                       String datetime = localDateTime.format(datetimeFormatter);
                                                       billDatetime.setText(datetime);

                                                       billMethod.setText("Paid by " + paymentMethod);

                                                       List<HashMap<String, Object>> productList = (List<HashMap<String, Object>>) document.get("selectedProducts");
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

                                                       double profit = 0;
                                                       for (CBProduct product : selectedProducts){
                                                           profit = profit + (product.getQuantity() * product.getProductProfit());
                                                       }
                                                       String formattedProfit = currencyFormatter.format(profit);
                                                       profitAmount.setText("Profit: " + formattedProfit);

                                                       detailAdapter.notifyDataSetChanged();

                                                       db.collection("user")
                                                               .document(customerId)
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

                                                                               if (image != null){
                                                                                   Glide.with(SRBillDetail.this).load(image)
                                                                                           .apply(RequestOptions.circleCropTransform()).into(customerImage);
                                                                               } else {
                                                                                   customerImage.setImageResource(R.drawable.null_profile_image);
                                                                               }
                                                                               customerName.setText(name);
                                                                               customerEmail.setText(email);
                                                                               String fullAddress = address + ", " + pos + ", " + country;
                                                                               customerAddress.setText(fullAddress);
                                                                               String dialCode = dial.split(" ")[0];
                                                                               String fullContact = dialCode + " " + contact;
                                                                               customerPhone.setText(fullContact);
                                                                           }
                                                                       }
                                                                   }
                                                               });
                                                   }
                                               }
                                           }
                                       });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        eReceiptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProgressDialog progressDialog = new ProgressDialog();
                progressDialog.show(getSupportFragmentManager(), "progressDialog");
                SharedPreferences preferences1 = getSharedPreferences("user", Context.MODE_PRIVATE);
                String user = preferences1.getString("uid", "");
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("user")
                        .document(user)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()){
                                    DocumentSnapshot document = task.getResult();
                                    if (document.exists()){
                                        String businessName = document.getString("userName");
                                        String businessEmail = document.getString("userEmail");
                                        String dial = document.getString("userDialCode");
                                        String contact = document.getString("userContact");
                                        String pos = document.getString("userPosCode");
                                        String country = document.getString("userCountry");
                                        String address = document.getString("userAddress");
                                        String fullAddress = address + ", " + pos + ", " + country;
                                        String dialCode = dial.split(" ")[0];
                                        String fullContact = dialCode + " " + contact;

                                        String cusName = customerName.getText().toString().trim();
                                        String cusEmail = customerEmail.getText().toString().trim();
                                        String rcptDateTime = billDatetime.getText().toString().trim();

                                        generateReceipt(businessName, fullAddress, fullContact, businessEmail,
                                                cusName, cusEmail, selectedProducts, rcptDateTime, progressDialog);
                                    }
                                }
                            }
                        });
            }
        });
    }
}
