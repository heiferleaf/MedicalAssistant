package com.whu.medicalbackend.common.exception;

public class PasswordIncorrectException extends BusinessException {
    public PasswordIncorrectException() {
        super("USER_003", "密码错误");
    }
}
