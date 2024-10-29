package com.example.billsphere;

public class SProduct {
    private String productImage;
    private String productName;
    private double productPrice;
    private String productCode;
    private String categoryId;
    private String productId;
    private boolean productStatus;

    public SProduct(){
    }

    public SProduct(String productImage, String productName, double productPrice, String categoryId,
                     String productId, String productCode, boolean productStatus){
        this.productImage = productImage;
        this.productName = productName;
        this.productPrice = productPrice;
        this.categoryId = categoryId;
        this.productId = productId;
        this.productCode = productCode;
        this.productStatus = productStatus;
    }

    public String getProductImage() {
        return productImage;
    }

    public void setProductImage(String productImage) {
        this.productImage = productImage;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public double getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(double productPrice) {
        this.productPrice = productPrice;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
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

    public boolean isProductStatus() {
        return productStatus;
    }

    public void setProductStatus(boolean productStatus) {
        this.productStatus = productStatus;
    }
}
