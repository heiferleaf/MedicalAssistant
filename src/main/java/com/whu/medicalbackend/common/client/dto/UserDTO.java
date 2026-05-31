package com.whu.medicalbackend.common.client.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String phone;
}