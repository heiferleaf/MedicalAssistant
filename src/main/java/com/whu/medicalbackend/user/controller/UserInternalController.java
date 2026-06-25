package com.whu.medicalbackend.user.controller;

import com.whu.medicalbackend.common.client.dto.UserDTO;
import com.whu.medicalbackend.common.response.Result;
import com.whu.medicalbackend.user.entity.User;
import com.whu.medicalbackend.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户内部 API 控制器
 *
 * 提供用户信息查询接口供其他微服务调用
 * 路径前缀：/internal/user
 *
 * 注意：此接口仅供内部服务调用，不对外暴露
 *
 * @author Medical Assistant Team
 */
@RestController
@RequestMapping("/internal/user")
public class UserInternalController {

    private static final Logger logger = LoggerFactory.getLogger(UserInternalController.class);

    @Autowired
    private UserMapper userMapper;

    /**
     * 根据用户 ID 查询用户信息
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    @GetMapping("/{userId}")
    public Result<UserDTO> getUserById(@PathVariable Long userId) {
        logger.debug("内部调用: 查询用户信息, userId={}", userId);

        User user = userMapper.findByUserId(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        UserDTO dto = convertToDTO(user);
        return Result.success(dto);
    }

    /**
     * 批量查询用户信息
     *
     * @param userIds 用户 ID 列表
     * @return 用户信息列表
     */
    @PostMapping("/batch")
    public Result<List<UserDTO>> getUsersByIds(@RequestBody List<Long> userIds) {
        logger.debug("内部调用: 批量查询用户信息, userIds={}", userIds);

        if (userIds == null || userIds.isEmpty()) {
            return Result.success(List.of());
        }

        // 逐个查询（如果需要批量查询，需要在 UserMapper 中添加 selectByIds 方法）
        List<UserDTO> dtos = userIds.stream()
                .map(userMapper::findByUserId)
                .filter(user -> user != null)
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return Result.success(dtos);
    }

    /**
     * 根据用户名查询用户信息
     *
     * @param username 用户名
     * @return 用户信息
     */
    @GetMapping("/by-username/{username}")
    public Result<UserDTO> getUserByUsername(@PathVariable String username) {
        logger.debug("内部调用: 根据用户名查询用户信息, username={}", username);

        User user = userMapper.findByUsername(username);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        UserDTO dto = convertToDTO(user);
        return Result.success(dto);
    }

    /**
     * 检查用户是否存在
     *
     * @param userId 用户 ID
     * @return 是否存在
     */
    @GetMapping("/{userId}/exists")
    public Result<Boolean> checkUserExists(@PathVariable Long userId) {
        logger.debug("内部调用: 检查用户是否存在, userId={}", userId);
        User user = userMapper.findByUserId(userId);
        return Result.success(user != null);
    }

    @GetMapping("/by-phone/{phone}")
    public Result<UserDTO> getUserByPhone(@PathVariable String phone) {
        logger.debug("内部调用: 根据手机号查询用户, phone={}", phone);
        User user = userMapper.findByPhoneNumber(phone);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(convertToDTO(user));
    }

    /**
     * 将 User 实体转换为 UserDTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setPhone(user.getPhoneNumber());
        // 注意：不传输密码等敏感信息
        return dto;
    }
}
