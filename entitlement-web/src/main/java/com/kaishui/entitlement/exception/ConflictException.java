package com.kaishui.entitlement.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// Maps to HTTP 409 Conflict
public class ConflictException extends ResponseStatusException {
    public ConflictException(String reason) {
        super(HttpStatus.CONFLICT, reason);
    }

    public ConflictException(String reason, Throwable cause) {
        super(HttpStatus.CONFLICT, reason, cause);
    }
}