package com.whu.medicalbackend.controller;

import com.whu.medicalbackend.dto.UserInfoDTO;
import com.whu.medicalbackend.service.serviceImpl.RedisService;
import com.whu.medicalbackend.common.Result;
import com.whu.medicalbackend.common.ResultCode;
import com.whu.medicalbackend.dto.UserLoginDto;
import com.whu.medicalbackend.dto.UserRegisterDto;
import com.whu.medicalbackend.dto.UserVO;
import com.whu.medicalbackend.entity.User;
import com.whu.medicalbackend.service.UserService;
import com.whu.medicalbackend.util.JwtUtil;
import com.whu.medicalbackend.util.RedisKeyBuilderUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Resource
    private RedisService redis;

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
        User user = userService.login(dto);
        String accessToken = JwtUtil.createAccessToken(user.getId());
        String refreshToken = UUID.randomUUID().toString();

        redis.setWithExpire(RedisKeyBuilderUtil.getAuthRefreshTokenKey(user.getId()), refreshToken, 7, TimeUnit.DAYS);

        // 2. 转换为VO
        UserVO userVO = new UserVO.Builder().
                        build(user, accessToken, refreshToken);

        // 3. 返回成功结果
        return Result.success("登录成功", userVO);
    }

    /**
     * 用户修改个人信息接口
     */
    @PutMapping("/modify")
    public Result<UserVO> modify(@Valid @RequestBody UserInfoDTO dto) {
        User user = userService.modify(dto);
        UserVO userVO = new UserVO.Builder().build(user);
        return Result.success(userVO);
    }

    @PostMapping("/refresh")
    public Result<String> refresh(@RequestBody Map<String, String> params) {
        String userId   = params.get("userId");
        String appRT    = params.get("refreshToken");

        log.info("appRT={}", appRT);

        String key      = RedisKeyBuilderUtil.getAuthRefreshTokenKey(userId);
        String serverRT = redis.get(key);

        if(serverRT != null && serverRT.equals(appRT)) {
            String newAccessToken = JwtUtil.createAccessToken(Long.valueOf(userId));
            return Result.success("获取新accessToken成功", newAccessToken);
        }
        return Result.error(ResultCode.NEED_LOGIN_ERROR);
    }
}