package com.appsdeveloperblog.payments.ws.core.error;

public class SpecificException extends Exception {
    public SpecificException() {
    }

    public SpecificException(Throwable cause) {
        super(cause);
    }

    public SpecificException(String message) {
        super(message);
    }
}
