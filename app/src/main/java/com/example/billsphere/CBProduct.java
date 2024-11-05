package com.example.billsphere;

import java.io.Serializable;
import java.util.ArrayList;

public class CBProduct implements Serializable {
    private String productImage;
    private String productName;
    private double productPrice;
    private String productCode;
    private String categoryId;
    private String productId;
    private int quantity;
    private double productProfit;

    public CBProduct(){
    }

    public CBProduct(String productImage, String productName, double productPrice, String categoryId,
                     String productId, double productProfit, String productCode){
        this.productImage = productImage;
        this.productName = productName;
        this.productPrice = productPrice;
        this.categoryId = categoryId;
        this.productId = productId;
        this.quantity = 0;
        this.productProfit = productProfit;
        this.productCode = productCode;
    }

    public String getProductImage() {
        return productImage;
    }

    public void setProductImage(String productImage) {
        this.productImage = productImage;
    }

    public double getProductProfit() {
        return productProfit;
    }

    public void setProductProfit(double productProfit) {
        this.productProfit = productProfit;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public double getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(double productPrice) {
        this.productPrice = productPrice;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }
}
