package com.whu.medicalbackend.exception;

public class UserPhoneAlreadyExistsException extends BusinessException{
    public UserPhoneAlreadyExistsException(String phoneNumber) {
        super("User phone number already exists: " + phoneNumber);
    }
}
