package com.vasu.notificationservice.exception;

import com.vasu.notificationservice.dto.common.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException ex) {
        return ErrorResponse.builder().status(HttpStatus.NOT_FOUND.value()).message(ex.getMessage()).timestamp(LocalDateTime.now()).build();
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.error("Database integrity violation: {}", ex.getMessage());
        if (ex.getMessage() != null && ex.getMessage().contains("duplicate")) {
            return ErrorResponse.builder().status(HttpStatus.CONFLICT.value())
                    .message("Duplicate entry. This resource already exists.")
                    .timestamp(LocalDateTime.now()).build();
        }
        return ErrorResponse.builder().status(HttpStatus.CONFLICT.value())
                .message("Data integrity constraint violated.")
                .timestamp(LocalDateTime.now()).build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ErrorResponse.builder().status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("An unexpected error occurred. Please try again later.")
                .timestamp(LocalDateTime.now()).build();
    }
}
