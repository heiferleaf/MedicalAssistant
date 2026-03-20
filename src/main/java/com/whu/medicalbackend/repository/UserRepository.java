package com.whu.medicalbackend.repository;

import com.whu.medicalbackend.entity.User;
import com.whu.medicalbackend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository{

    @Autowired
    private UserMapper userMapper;

    /**
     * 保存用户
     *
     * @param user 用户对象
     * @return 保存后的用户（含数据库生成的ID）
     */
    public User save(User user) {
        // MyBatis会自动将生成的ID回填到user对象
        userMapper.insert(user);
        return user;
    }

    /**
     * 根据用户名查找用户
     */
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    /**
     * 检查用户名是否存在
     */
    public boolean existsByUsername(String username) {
        return userMapper.existsByUsername(username) > 0;
    }

    /**
     * 检查用户手机是否存在
     */
    public boolean existsByUserPhoneNumber(String phoneNumber) {
        return userMapper.existsByUserPhoneNumber(phoneNumber) > 0;
    }

    /**
     * 修改用户信息
     */
    public int updateUserInfo(User user) {
        return userMapper.updateUserInfo(user);
    }
}
