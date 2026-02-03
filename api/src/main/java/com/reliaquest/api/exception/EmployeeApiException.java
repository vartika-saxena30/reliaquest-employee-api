package com.reliaquest.api.exception;

public class EmployeeApiException extends RuntimeException {

    public EmployeeApiException(String message) {
        super(message);
    }

    public EmployeeApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
