package com.example.billsphere;

import java.util.ArrayList;

public class ProductCartManager {
    private static ProductCartManager instance;
    private ArrayList<CBProduct> selectedProducts;

    private ProductCartManager() {
        selectedProducts = new ArrayList<>();
    }

    public static ProductCartManager getInstance() {
        if (instance == null) {
            instance = new ProductCartManager();
        }
        return instance;
    }

    public ArrayList<CBProduct> getSelectedProducts() {
        return selectedProducts;
    }

    public void addProduct(CBProduct product) {
        selectedProducts.add(product);
    }

    public void removeProduct(CBProduct product) {
        selectedProducts.remove(product);
    }

    public void clearProducts() {
        selectedProducts.clear();
    }

    public int getProductCount() {
        return selectedProducts.size();
    }
}
