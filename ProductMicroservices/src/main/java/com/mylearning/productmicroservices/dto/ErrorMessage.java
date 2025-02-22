package com.mylearning.productmicroservices.dto;

import java.util.Date;

public class ErrorMessage {

    private Date time;
    private String errorMessage;
    private String stackTrace;
    private String details;


    public ErrorMessage() {
    }

    public ErrorMessage(Date time, String errorMessage, String stackTrace, String details) {
        this.time = time;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
        this.details = details;
    }
}
