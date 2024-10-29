package com.example.billsphere;

public class Category {
    int categoryId;
    String categoryName;

    public Category(){
    }

    public Category(int i, String n){
        this.categoryId = i;
        this.categoryName = n;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }
}
