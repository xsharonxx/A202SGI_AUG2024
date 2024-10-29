package com.example.billsphere;

public class Product {
    private String productImage;
    private String productName;
    private String productCode;
    private String categoryId;
    private int stock;
    private double cost;
    private double price;
    private double profit;
    private boolean productStatus;
    private String productId;

    public Product(){
    }

    public Product(String productImage, String productName, String productCode, String categoryId, int stock,
                   double cost, double price, double profit, boolean productStatus, String productId){
        this.productImage = productImage;
        this.productName = productName;
        this.productCode = productCode;
        this.categoryId = categoryId;
        this.stock = stock;
        this.cost = cost;
        this.price = price;
        this.profit = profit;
        this.productStatus = productStatus;
        this.productId = productId;
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

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getProfit() {
        return profit;
    }

    public void setProfit(double profit) {
        this.profit = profit;
    }

    public boolean isProductStatus() {
        return productStatus;
    }

    public void setProductStatus(boolean productStatus) {
        this.productStatus = productStatus;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }
}
