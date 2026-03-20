package com.whu.medicalbackend.service;

import com.whu.medicalbackend.dto.UserInfoDTO;
import com.whu.medicalbackend.dto.UserLoginDto;
import com.whu.medicalbackend.dto.UserRegisterDto;
import com.whu.medicalbackend.entity.User;

public interface UserService{
    User register(UserRegisterDto dto);
    User login(UserLoginDto dto);
    User modify(UserInfoDTO user);
}
