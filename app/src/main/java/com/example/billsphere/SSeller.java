package com.example.billsphere;

public class SSeller {
    private String businessImage;
    private String businessName;
    private String businessEmail;
    private String businessContact;
    private String businessDial;
    private String businessAddress;
    private String businessPos;
    private String businessCountry;
    private String businessId;

    public SSeller() {
    }

    public SSeller(String businessImage, String businessName, String businessEmail, String businessContact,
                      String businessDial, String businessAddress, String businessPos, String businessCountry,
                      String businessId) {
        this.businessImage = businessImage;
        this.businessName = businessName;
        this.businessEmail = businessEmail;
        this.businessContact = businessContact;
        this.businessDial = businessDial;
        this.businessAddress = businessAddress;
        this.businessPos = businessPos;
        this.businessCountry = businessCountry;
        this.businessId = businessId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getBusinessImage() {
        return businessImage;
    }

    public void setBusinessImage(String businessImage) {
        this.businessImage = businessImage;
    }

    public String getBusinessEmail() {
        return businessEmail;
    }

    public void setBusinessEmail(String businessEmail) {
        this.businessEmail = businessEmail;
    }

    public String getBusinessContact() {
        return businessContact;
    }

    public void setBusinessContact(String businessContact) {
        this.businessContact = businessContact;
    }

    public String getBusinessDial() {
        return businessDial;
    }

    public void setBusinessDial(String businessDial) {
        this.businessDial = businessDial;
    }

    public String getBusinessAddress() {
        return businessAddress;
    }

    public void setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
    }

    public String getBusinessPos() {
        return businessPos;
    }

    public void setBusinessPos(String businessPos) {
        this.businessPos = businessPos;
    }

    public String getBusinessCountry() {
        return businessCountry;
    }

    public void setBusinessCountry(String businessCountry) {
        this.businessCountry = businessCountry;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }
}
