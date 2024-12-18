package com.example.billsphere;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class BHReceipt {
    private String businessId;
    private String paymentMethod;
    private LocalDateTime timestamp;
    private double totalAmount;
    private ArrayList<CBProduct> selectedProductList;
    private String receiptId;
    private String userName;
    private String userEmail;

    public BHReceipt(){
    }

    public BHReceipt(String businessId, String paymentMethod, double totalAmount, LocalDateTime timestamp,
                     ArrayList<CBProduct> selectedProductList, String receiptId){
        this.businessId = businessId;
        this.paymentMethod = paymentMethod;
        this.totalAmount = totalAmount;
        this.timestamp = timestamp;
        this.selectedProductList = selectedProductList;
        this.receiptId = receiptId;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public ArrayList<CBProduct> getSelectedProductList() {
        return selectedProductList;
    }

    public void setSelectedProductList(ArrayList<CBProduct> selectedProductList) {
        this.selectedProductList = selectedProductList;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(String receiptId) {
        this.receiptId = receiptId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
}
