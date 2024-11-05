package com.example.billsphere;

public class CBCustomer {
    private String customerImage;
    private String customerName;
    private String customerEmail;
    private String customerContact;
    private String customerDial;
    private String customerAddress;
    private String customerPos;
    private String customerCountry;
    private String customerId;

    public CBCustomer() {
    }

    public CBCustomer(String customerImage, String customerName, String customerEmail, String customerContact,
                      String customerDial, String customerAddress, String customerPos, String customerCountry,
                      String customerId) {
        this.customerImage = customerImage;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerContact = customerContact;
        this.customerDial = customerDial;
        this.customerAddress = customerAddress;
        this.customerPos = customerPos;
        this.customerCountry = customerCountry;
        this.customerId = customerId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerImage() {
        return customerImage;
    }

    public void setCustomerImage(String customerImage) {
        this.customerImage = customerImage;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerContact() {
        return customerContact;
    }

    public void setCustomerContact(String customerContact) {
        this.customerContact = customerContact;
    }

    public String getCustomerDial() {
        return customerDial;
    }

    public void setCustomerDial(String customerDial) {
        this.customerDial = customerDial;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public String getCustomerPos() {
        return customerPos;
    }

    public void setCustomerPos(String customerPos) {
        this.customerPos = customerPos;
    }

    public String getCustomerCountry() {
        return customerCountry;
    }

    public void setCustomerCountry(String customerCountry) {
        this.customerCountry = customerCountry;
    }
}
