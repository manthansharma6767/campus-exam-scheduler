package com.manthan.campusexamscheduler.exception;

import com.manthan.campusexamscheduler.dto.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(message.isBlank() ? "Validation failed" : message)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestPartException.class})
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, "The request contains missing or invalid values.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "The uploaded file exceeds the 10 MB limit.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        // Surface a clear 409 instead of a raw 500 for unique-constraint violations
        String msg = "A record with these details already exists.";
        String root = ex.getRootCause() != null && ex.getRootCause().getMessage() != null
                ? ex.getRootCause().getMessage()
                : "";
        if (root.toLowerCase().contains("email")) msg = "Email address is already in use.";
        else if (root.toLowerCase().contains("enrollment")) msg = "Enrollment number is already in use.";
        else if (root.toLowerCase().contains("subject_code") || root.toLowerCase().contains("subjectcode")) msg = "Subject code is already in use.";
        else if (root.toLowerCase().contains("department_name") || root.toLowerCase().contains("departmentname")) msg = "Department name is already in use.";
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .message(msg)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex) {

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex) {

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("An unexpected error occurred. Please try again.")
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(error,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        ErrorResponse error = ErrorResponse.builder()
                .status(status.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, status);
    }
}
