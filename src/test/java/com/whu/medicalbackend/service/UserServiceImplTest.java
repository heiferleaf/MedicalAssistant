package com.whu.medicalbackend.service;

import com.whu.medicalbackend.user.entity.dto.UserLoginDto;
import com.whu.medicalbackend.user.entity.dto.UserRegisterDto;
import com.whu.medicalbackend.user.entity.User;
import com.whu.medicalbackend.common.exception.PasswordIncorrectException;
import com.whu.medicalbackend.common.exception.UserAlreadyExistsException;
import com.whu.medicalbackend.common.exception.UserNotFoundException;
import com.whu.medicalbackend.user.repository.UserRepository;
import com.whu.medicalbackend.user.service.serviceImpl.UserServiceImpl;
import com.whu.medicalbackend.common.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api. Test;
import org.junit. jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api. Assertions.*;
import static org. mockito.ArgumentMatchers.*;
import static org.mockito. Mockito.*;

/**
 * UserService单元测试
 * Spring测试知识点：
 * 1. @ExtendWith(MockitoExtension.class)：启用Mockito支持
 * 2. @Mock：创建Mock对象
 * 3. @InjectMocks：自动注入Mock对象到被测试类
 * 4. when(... ).thenReturn(...)：模拟方法返回值
 * 5. verify(...)：验证方法是否被调用
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;  // Mock依赖

    @InjectMocks
    private UserServiceImpl userService;    // 被测试的对象（自动注入Mock的Repository）

    private UserRegisterDto registerDTO;
    private UserLoginDto loginDTO;
    private User mockUser;

    /**
     * 每个测试方法执行前都会执行
     * Java知识点：@BeforeEach（JUnit5生命周期）
     */
    @BeforeEach
    void setUp() {
        // 准备测试数据
        registerDTO = new UserRegisterDto();
        registerDTO.setUsername("zhangsan");
        registerDTO.setPassword("123456");
        registerDTO.setNickname("张三");

        loginDTO = new UserLoginDto();
        loginDTO.setUsername("zhangsan");
        loginDTO.setPassword("123456");

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("zhangsan");
        mockUser.setPassword(PasswordUtil.encrypt("123456"));
        mockUser.setNickname("张三");
    }

    // ==================== 注册测试 ====================

    /**
     * 测试：注册成功
     */
    @Test
    void should_RegisterSuccess_When_UsernameNotExists() {
        // Given:  模拟用户名不存在
        when(userRepository.existsByUsername("zhangsan")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // When: 执行注册
        User result = userService.register(registerDTO);

        // Then: 验证结果
        assertNotNull(result, "注册结果不应为null");
        assertEquals("zhangsan", result.getUsername());
        assertEquals("张三", result.getNickname());

        // 验证Repository方法被调用
        verify(userRepository, times(1)).existsByUsername(eq("zhangsan"));
        verify(userRepository, times(1)).save(any(User.class));
    }

    /**
     * 测试：注册失败 - 用户名已存在
     */
    @Test
    void should_ThrowException_When_UsernameAlreadyExists() {
        // Given: 模拟用户名已存在
        when(userRepository.existsByUsername("zhangsan")).thenReturn(true);

        // When & Then: 执行注册应该抛出异常
        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.register(registerDTO),
                "用户名已存在应该抛出UserAlreadyExistsException"
        );

        // 验证异常消息
        assertTrue(exception.getMessage().contains("zhangsan"));

        // 验证save方法没有被调用
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== 登录测试 ====================

    /**
     * 测试：登录成功
     */
    @Test
    void should_LoginSuccess_When_UsernameAndPasswordCorrect() {
        // Given:  模拟用户存在且密码正确
        when(userRepository.findByUsername("zhangsan")).thenReturn(mockUser);

        // When: 执行登录
        User result = userService.login(loginDTO);

        // Then: 验证结果
        assertNotNull(result);
        assertEquals("zhangsan", result.getUsername());
        assertEquals("张三", result.getNickname());

        // 验证Repository方法被调用
        verify(userRepository, times(1)).findByUsername("zhangsan");
    }

    /**
     * 测试：登录失败 - 用户不存在
     */
    @Test
    void should_ThrowException_When_UserNotFound() {
        // Given: 模拟用户不存在
        when(userRepository.findByUsername("zhangsan")).thenReturn(null);

        // When & Then: 执行登录应该抛出异常
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.login(loginDTO),
                "用户不存在应该抛出UserNotFoundException"
        );

        assertTrue(exception.getMessage().contains("zhangsan"));
    }

    /**
     * 测试：登录失败 - 密码错误
     */
    @Test
    void should_ThrowException_When_PasswordIncorrect() {
        // Given:  模拟用户存在但密码错误
        when(userRepository.findByUsername("zhangsan")).thenReturn(mockUser);
        loginDTO.setPassword("wrongPassword");

        // When & Then: 执行登录应该抛出异常
        PasswordIncorrectException exception = assertThrows(
                PasswordIncorrectException.class,
                () -> userService.login(loginDTO),
                "密码错误应该抛出PasswordIncorrectException"
        );

        assertTrue(exception.getMessage().contains("密码错误"));
    }
}