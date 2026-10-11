package com.vasu.reviewservice.exception;

public class BookingVerificationException extends RuntimeException {

    public BookingVerificationException(String message) {
        super(message);
    }

    public BookingVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
