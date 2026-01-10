package com.whu.medicalbackend.exception;

public class UserNotFoundException extends BusinessException{
    public UserNotFoundException(String username) {
        super("USER_002", "User not Found:" + username);
    }
}
