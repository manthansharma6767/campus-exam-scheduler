package com.manthan.campusexamscheduler.exception;

/**
 * Raised when an enrollment/DOB check or a password check fails.
 * Mapped to HTTP 401 so callers can distinguish it from malformed input.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
