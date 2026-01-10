package com.whu.medicalbackend.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordUtil {
    private static final int STRENGTH = 5; // 强度设为5，进行32次哈希迭代计算，兼顾性能和安全
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(STRENGTH);

    /**
     * 加密明文密码
     */
    public static String encrypt(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return encoder.encode(rawPassword);
    }

    /**
     * 验证密码是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return encoder.matches(rawPassword, encodedPassword);
    }
}
