package com.vasu.movie_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class OverlappingShowException extends RuntimeException {

    public OverlappingShowException(String message) {
        super(message);
    }
}
