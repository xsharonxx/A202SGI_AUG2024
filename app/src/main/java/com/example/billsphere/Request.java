package com.example.billsphere;

import java.time.LocalDateTime;

public class Request {
    private String requestId;
    private String userId;
    private String userName;
    private String userEmail;
    private LocalDateTime requestTimestamp;
    private String rejectReason;
    private LocalDateTime acceptRejectTimestamp;
    private String businessStatus;

    public Request(){
    }

    public Request(String requestId, String userId, LocalDateTime requestTimestamp){
        this.requestId = requestId;
        this.userId = userId;
        this.requestTimestamp = requestTimestamp;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public LocalDateTime getRequestTimestamp() {
        return requestTimestamp;
    }

    public void setRequestTimestamp(LocalDateTime requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public LocalDateTime getAcceptRejectTimestamp() {
        return acceptRejectTimestamp;
    }

    public void setAcceptRejectTimestamp(LocalDateTime acceptRejectTimestamp) {
        this.acceptRejectTimestamp = acceptRejectTimestamp;
    }

    public String getBusinessStatus() {
        return businessStatus;
    }

    public void setBusinessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
    }
}
