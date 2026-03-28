package com.whu.medicalbackend.common.exception;

public class UserNotFoundException extends BusinessException{
    public UserNotFoundException(String username) {
        super("USER_002", "User not Found:" + username);
    }
}
