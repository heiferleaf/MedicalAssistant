package com.whu.medicalbackend.common.exception;

/**
 * BusinessException: 这是业务异常类的基类
 */
public class BusinessException extends RuntimeException {
    private String errorCode;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getErrorCode() { return this.errorCode; }
}
