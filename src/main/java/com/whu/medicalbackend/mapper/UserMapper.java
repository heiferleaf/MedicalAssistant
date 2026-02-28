package com.whu.medicalbackend.mapper;

import com.whu.medicalbackend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户Mapper接口
 * MyBatis知识点：
 * 1. @Mapper：标识为MyBatis映射接口，Spring自动代理
 * 2. 接口方法对应XML中的SQL语句（通过id匹配）
 * 3. @Param：指定参数名称（用于XML中的#{参数名}）
 */
@Mapper
public interface UserMapper{

    /**
     * 插入用户
     *
     * @param user 用户对象
     * @return 影响的行数
     */
    int insert(User user);

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 用户对象，不存在返回null
     */
    User findByUsername(@Param("username") String username);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 存在返回1，不存在返回0
     */
    int existsByUsername(@Param("username") String username);

    /**
     * 检查用户手机号是否存在
     *
     * @param username 用户手机
     * @return 存在返回1，不存在返回0
     */
    int existsByUserPhoneNumber(@Param("phoneNumber") String phoneNumber);

    /**
     * 根据userId寻找用户
     * @param userId
     * @return
     */
    User findByUserId(@Param("userId") Long userId);

    /**
     * 根据手机号找用户
     * @param phoneNumber
     * @return
     */
    User findByPhoneNumber(@Param("phoneNumber") String phoneNumber);
}