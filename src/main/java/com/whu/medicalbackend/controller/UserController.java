package com.whu.medicalbackend.controller;

import com.whu.medicalbackend.common.Result;
import com.whu.medicalbackend.dto.UserLoginDto;
import com.whu.medicalbackend.dto.UserRegisterDto;
import com.whu.medicalbackend.dto.UserVO;
import com.whu.medicalbackend.entity.User;
import com.whu.medicalbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册接口
     * URL: POST /api/user/register
     * @param dto 注册信息（自动校验）
     * @return 统一响应Result
     */
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody UserRegisterDto dto) {
        // 1. 调用Service注册
        User user = userService.register(dto);

        // 2. 转换为VO（不返回密码）
        UserVO userVO = new UserVO.Builder().build(user);

        // 3. 返回成功结果
        return Result.success("注册成功", userVO);
    }

    /**
     * 用户登录接口
     * URL: POST /api/user/login
     */
    @PostMapping("/login")
    public Result<UserVO> login(@Valid @RequestBody UserLoginDto dto) {
        // 1. 调用Service登录
        User user = userService. login(dto);

        // 2. 转换为VO
        UserVO userVO = new UserVO.Builder().build(user);

        // 3. 返回成功结果
        return Result.success("登录成功", userVO);
    }
}