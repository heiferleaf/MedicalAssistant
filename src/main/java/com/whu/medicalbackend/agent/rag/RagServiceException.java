package com.whu.medicalbackend.agent.rag;

import org.springframework.http.HttpStatus;

public class RagServiceException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public RagServiceException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public RagServiceException(HttpStatus status, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
