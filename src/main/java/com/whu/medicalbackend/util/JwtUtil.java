package com.whu.medicalbackend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.Optional;

public class JwtUtil {
    private static final String SECRET = "Medicine_Backend_WHU_ABAABA_2026";

    public static String createAccessToken(Long userId) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .setExpiration(new Date(System.currentTimeMillis() + 30 * 60 * 1000))
                .signWith(SignatureAlgorithm.ES256, SECRET)
                .compact();
    }

    public static Optional<Claims> parseToken(String token) {
        try {
            return Optional.ofNullable(Jwts.parser().setSigningKey(SECRET).parseClaimsJwt(token).getBody());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
