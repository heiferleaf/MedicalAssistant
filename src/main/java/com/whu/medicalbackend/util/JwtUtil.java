package com.whu.medicalbackend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

public class JwtUtil {
    // HS256 算法要求密钥至少 32 位字符（256位），你这个字符串正好符合
    private static final String SECRET = "Medicine_Backend_WHU_ABAABA_2026";

    // 将字符串转为安全密钥对象
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public static String createAccessToken(Long userId) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .setExpiration(new Date(System.currentTimeMillis() + 30 * 60 * 1000))
                // 1. 修改算法为 HS256
                // 2. 传入包装后的 KEY
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static Optional<Claims> parseToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(KEY) // 使用相同的 KEY 进行解密
                    .build()
                    .parseClaimsJws(token) // 注意：带签名的 Token 使用 parseClaimsJws
                    .getBody();
            return Optional.ofNullable(claims);
        } catch (Exception e) {
            // 这里可以增加打印错误日志，方便调试 e.printStackTrace();
            return Optional.empty();
        }
    }
}