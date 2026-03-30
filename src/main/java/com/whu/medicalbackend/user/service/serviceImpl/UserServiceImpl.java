package com.whu.medicalbackend.user.service.serviceImpl;

import com.whu.medicalbackend.common.exception.*;
import com.whu.medicalbackend.family.service.FamilyCacheService;
import com.whu.medicalbackend.user.dto.UserInfoDTO;
import com.whu.medicalbackend.user.dto.UserLoginDto;
import com.whu.medicalbackend.user.dto.UserRegisterDto;
import com.whu.medicalbackend.user.entity.User;
import com.whu.medicalbackend.family.mapper.FamilyMemberMapper;
import com.whu.medicalbackend.user.repository.UserRepository;
import com.whu.medicalbackend.user.service.UserService;
import com.whu.medicalbackend.common.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(rollbackFor = Exception.class)
public class UserServiceImpl implements UserService{
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FamilyMemberMapper familyMemberMapper;
    @Autowired
    private FamilyCacheService familyCacheService;

    @Override
    public User register(UserRegisterDto dto) {
        // 1. 检查注册的用户名或者手机号已经存在
        if(userRepository.existsByUsername(dto.getUsername())) {
            throw new UserAlreadyExistsException(dto.getUsername());
        }
        if(userRepository.existsByUserPhoneNumber(dto.getPhoneNumber())) {
            throw new UserPhoneAlreadyExistsException(dto.getPhoneNumber());
        }

        // 2. 创建用户对象
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(PasswordUtil.encrypt(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setPhoneNumber(dto.getPhoneNumber());

        // 3. 保存用户（MyBatis会自动回填ID）
        return userRepository.save(user);
    }

    @Override
    public User login(UserLoginDto dto) {

        User user = userRepository.findByUsername(dto.getUsername());

        if(user == null) {
            throw new UserNotFoundException(dto.getUsername());
        }

        assert(user.getPassword() != null) : "empty password";
        if(!PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new PasswordIncorrectException();
        }

        return user;
    }

    @Override
    public User modify(UserInfoDTO dto) {
        User user = userRepository.findByUsername(dto.getUsername());
        if(user == null || !user.getId().equals(dto.getId())) {
            throw new BusinessException("user not found");
        }
        user.setNickname(dto.getNewNickname());
        user.setPassword(PasswordUtil.encrypt(dto.getNewPassword()));
        user.setPhoneNumber(dto.getNewPhoneNumber());
        user.setUpdateTime(LocalDateTime.now());
        if(userRepository.updateUserInfo(user) != 1) {
            throw new BusinessException("修改用户信息失败");
        }
        return user;
    }
}
