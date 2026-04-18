package com.whu.medicalbackend.common.exception;

public class UserAlreadyExistsException extends BusinessException{
    public UserAlreadyExistsException(String username) {
        super("USER_001", "User already exists:" + username);
    }
}
