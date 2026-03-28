package com.whu.medicalbackend.user.service;

import com.whu.medicalbackend.user.entity.dto.UserInfoDTO;
import com.whu.medicalbackend.user.entity.dto.UserLoginDto;
import com.whu.medicalbackend.user.entity.dto.UserRegisterDto;
import com.whu.medicalbackend.user.entity.User;

public interface UserService{
    User register(UserRegisterDto dto);
    User login(UserLoginDto dto);
    User modify(UserInfoDTO user);
}
