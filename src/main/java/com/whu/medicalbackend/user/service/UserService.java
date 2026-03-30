package com.whu.medicalbackend.user.service;

import com.whu.medicalbackend.user.dto.UserInfoDTO;
import com.whu.medicalbackend.user.dto.UserLoginDto;
import com.whu.medicalbackend.user.dto.UserRegisterDto;
import com.whu.medicalbackend.user.entity.User;

public interface UserService{
    User register(UserRegisterDto dto);
    User login(UserLoginDto dto);
    User modify(UserInfoDTO user);
}
