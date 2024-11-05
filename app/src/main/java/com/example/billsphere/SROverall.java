package com.example.billsphere;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SROverall extends Fragment {

    private ImageButton filterButton;
    private BarChart barChart;
    private TextView date, paymentMethod, billsAmount, salesAmount, profitAmount;

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
                updateSalesReport();
                dialog.dismiss();
            }
        });

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateSalesReport(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences preferences1 = getActivity().getSharedPreferences("user", Context.MODE_PRIVATE);
        String user = preferences1.getString("uid", "");
        CollectionReference salesRef = db.collection("bill");
        Query query = salesRef;
        query = query.whereEqualTo("businessId", user);
        if (selectedFilter[0] > 0) {
            String selectedPaymentMethod = getResources().getStringArray(R.array.payment_methods)[selectedFilter[0] - 1];
            query = query.whereEqualTo("paymentMethod", selectedPaymentMethod);
        }

        // Define the date range for the last 30 days
        LocalDateTime now = LocalDateTime.now();
        Timestamp endTimestamp = Timestamp.now();

        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -30);
        Timestamp startTimestamp = new Timestamp(calendar.getTime());

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        // Create a list to hold all days within the range
        ArrayList<String> allDates = new ArrayList<>();
        for (LocalDateTime date = thirtyDaysAgo; !date.isAfter(now); date = date.plusDays(1)) {
            allDates.add(date.format(dateFormatter));
        }

        query = query.whereGreaterThanOrEqualTo("timestamp", startTimestamp)
                .whereLessThanOrEqualTo("timestamp", endTimestamp);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                if (!task.getResult().isEmpty()) {
                    Map<String, Float> dailySalesMap = new HashMap<>();
                    Map<String, Integer> dailyBillCountMap = new HashMap<>();
                    Map<String, Double> dailyProfitCountMap = new HashMap<>();

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Double amountDouble = document.getDouble("totalAmount");

                        List<HashMap<String, Object>> productList = (List<HashMap<String, Object>>) document.get("selectedProducts");
                        Double totalProfit = 0d;
                        for (HashMap<String, Object> product : productList) {
                            Long quantity = (Long) product.get("quantity");
                            Double profit = (Double) product.get("productProfit");
                            totalProfit += (quantity * profit);
                        }
                        Timestamp timestamp = document.getTimestamp("timestamp");
                        LocalDateTime localDateTime = null;
                        if (timestamp != null) {
                            localDateTime = timestamp.toDate()
                                    .toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime();
                        }

                        String date = localDateTime.format(dateFormatter);

                        dailySalesMap.put(date, dailySalesMap.getOrDefault(date, 0f) + amountDouble.floatValue());
                        dailyBillCountMap.put(date, dailyBillCountMap.getOrDefault(date, 0) + 1);
                        dailyProfitCountMap.put(date, dailyProfitCountMap.getOrDefault(date, 0d) + totalProfit);
                    }

                    ArrayList<BarEntry> barEntries = new ArrayList<>();
                    ArrayList<String> xValues = new ArrayList<>();
                    int index = 0;
                    for (String date : allDates) {
                        float amount = dailySalesMap.getOrDefault(date, 0f); // Get sales or 0 if not present
                        barEntries.add(new BarEntry(index++, amount));
                        xValues.add(date); // Store dates for x-axis labels
                    }

                    // Update the chart with the filtered data
                    BarDataSet dataSet = new BarDataSet(barEntries, "Sales");
                    dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.light_green));
                    dataSet.setHighLightColor(ContextCompat.getColor(requireContext(), R.color.teal_green));
                    BarData barData = new BarData(dataSet);
                    barChart.setData(barData);
                    barChart.getDescription().setEnabled(false);
                    barChart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);

                    // Set up x-axis labels
                    XAxis xAxis = barChart.getXAxis();
                    xAxis.setValueFormatter(new IndexAxisValueFormatter(xValues)); // Set x-axis labels
                    xAxis.setGranularity(1f); // Set granularity to 1 to show each label
                    xAxis.setLabelRotationAngle(45f); // Rotate labels for better visibility
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                    barChart.invalidate();

                    barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                        private Entry selectedEntry = null;

                        @Override
                        public void onValueSelected(Entry e, Highlight h) {
                            selectedEntry = e;

                            if (selectedFilter[0] > 0) {
                                String selectedPaymentMethod = getResources().getStringArray(R.array.payment_methods)[selectedFilter[0] - 1];
                                paymentMethod.setText(selectedPaymentMethod);
                            } else {
                                paymentMethod.setText("All Payment Methods");
                            }

                            float selectedValue = e.getY();
                            String selectedDate = xValues.get((int) e.getX());

                            date.setText(selectedDate);
                            date.setPaintFlags(date.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

                            Locale locale = new Locale("ms", "MY");
                            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
                            String formattedTotal = currencyFormatter.format(selectedValue);
                            salesAmount.setText(formattedTotal);

                            Double profit = dailyProfitCountMap.getOrDefault(selectedDate, 0d);
                            String formattedProfit = currencyFormatter.format(profit);
                            profitAmount.setText(formattedProfit);

                            int billCount = dailyBillCountMap.getOrDefault(selectedDate, 0);
                            billsAmount.setText(String.valueOf(billCount));
                        }

                        @Override
                        public void onNothingSelected() {
                            barChart.highlightValue(selectedEntry.getX(), 0);
                        }
                    });

                    float lastIndex = barEntries.size() - 1;
                    barChart.highlightValue(lastIndex, 0);
                } else {
                    ArrayList<BarEntry> barEntries = new ArrayList<>();
                    ArrayList<String> xValues = new ArrayList<>();
                    int index = 0;
                    for (String date : allDates) {
                        barEntries.add(new BarEntry(index++, 0f));
                        xValues.add(date); // Store dates for x-axis labels
                    }

                    // Update the chart with the filtered data
                    BarDataSet dataSet = new BarDataSet(barEntries, "Sales");
                    dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.light_green));
                    dataSet.setHighLightColor(ContextCompat.getColor(requireContext(), R.color.teal_green));
                    BarData barData = new BarData(dataSet);
                    barChart.setData(barData);
                    barChart.getDescription().setEnabled(false);
                    barChart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);

                    // Set up x-axis labels
                    XAxis xAxis = barChart.getXAxis();
                    xAxis.setValueFormatter(new IndexAxisValueFormatter(xValues)); // Set x-axis labels
                    xAxis.setGranularity(1f); // Set granularity to 1 to show each label
                    xAxis.setLabelRotationAngle(45f); // Rotate labels for better visibility
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                    barChart.invalidate();

                    barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                        private Entry selectedEntry = null;

                        @Override
                        public void onValueSelected(Entry e, Highlight h) {
                            selectedEntry = e;

                            if (selectedFilter[0] > 0) {
                                String selectedPaymentMethod = getResources().getStringArray(R.array.payment_methods)[selectedFilter[0] - 1];
                                paymentMethod.setText(selectedPaymentMethod);
                            } else {
                                paymentMethod.setText("All Payment Methods");
                            }

                            float selectedValue = e.getY();
                            String selectedDate = xValues.get((int) e.getX());

                            date.setText(selectedDate);
                            date.setPaintFlags(date.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

                            Locale locale = new Locale("ms", "MY");
                            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
                            String formattedTotal = currencyFormatter.format(selectedValue);
                            salesAmount.setText(formattedTotal);

                            String formattedProfit = currencyFormatter.format(0d);
                            profitAmount.setText(formattedProfit);

                            billsAmount.setText(String.valueOf(0));
                        }

                        @Override
                        public void onNothingSelected() {
                            barChart.highlightValue(selectedEntry.getX(), 0);
                        }
                    });

                    float lastIndex = barEntries.size() - 1;
                    barChart.highlightValue(lastIndex, 0);
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sr_overall, container, false);

        filterButton = view.findViewById(R.id.filter_button);
        barChart = view.findViewById(R.id.bar_chart);
        date = view.findViewById(R.id.date);
        paymentMethod = view.findViewById(R.id.payment_method);
        billsAmount = view.findViewById(R.id.bill_amount);
        salesAmount = view.findViewById(R.id.sales_amount);
        profitAmount = view.findViewById(R.id.profit_amount);

        updateSalesReport();

        filterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFilterDialog();
            }
        });

        return view;
    }
}
