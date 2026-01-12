package com.whu.medicalbackend.service.serviceImpl;

import com.whu.medicalbackend.dto.UserLoginDto;
import com.whu.medicalbackend.dto.UserRegisterDto;
import com.whu.medicalbackend.entity.User;
import com.whu.medicalbackend.exception.PasswordIncorrectException;
import com.whu.medicalbackend.exception.UserAlreadyExistsException;
import com.whu.medicalbackend.exception.UserNotFoundException;
import com.whu.medicalbackend.repository.UserRepository;
import com.whu.medicalbackend.service.UserService;
import com.whu.medicalbackend.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService{
    @Autowired
    private UserRepository userRepository;

    @Override
    public User register(UserRegisterDto dto) {
        // 1. 检查注册的用户名已经存在
        if(userRepository.existsByUsername(dto.getUsername())) {
            throw new UserAlreadyExistsException(dto.getUsername());
        }

        // 2. 创建用户对象
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(PasswordUtil.encrypt(dto.getPassword()));
        user.setNickname(dto.getNickname());  // 新增：设置昵称

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
}
