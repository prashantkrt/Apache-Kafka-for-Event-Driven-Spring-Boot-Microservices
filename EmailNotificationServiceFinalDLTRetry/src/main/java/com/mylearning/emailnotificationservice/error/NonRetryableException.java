package com.mylearning.emailnotificationservice.error;

public class NonRetryableException extends RuntimeException{

    public NonRetryableException(Throwable cause) {
        super(cause);
    }

    public NonRetryableException(String message) {
        super(message);
    }
}
