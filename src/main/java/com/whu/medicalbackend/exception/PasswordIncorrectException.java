package com.whu.medicalbackend.exception;

public class PasswordIncorrectException extends BusinessException {
    public PasswordIncorrectException() {
        super("USER_003", "incorrect password");
    }
}
