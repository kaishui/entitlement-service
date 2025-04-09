package com.kaishui.entitlement.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ResourceNotFoundException extends ResponseStatusException {
    public ResourceNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }

    public ResourceNotFoundException(String reason, Throwable cause) {
        super(HttpStatus.NOT_FOUND, reason, cause);
    }
}
