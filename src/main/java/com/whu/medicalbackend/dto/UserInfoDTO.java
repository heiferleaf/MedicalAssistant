package com.whu.medicalbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserInfoDTO {
    @NotNull
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "用户名长度需要是6-20")
    @Pattern(regexp = "^[0-9a-zA-Z_]+$", message = "用户名只能包含大小写字母，数字、以及_")
    private String username;

    @NotBlank
    @Pattern(regexp = "^.{6,20}$", message = "昵称长度为6-20位")
    @Pattern(regexp = "^[0-9a-zA-Z_]+$", message = "昵称只能包含大小写字母，数字、以及_")
    private String newNickname;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "密码长度为6-20位")
    @Pattern(regexp = "^[0-9a-zA-Z_]+$", message = "密码只能包含大小写字母，数字、以及_")
    private String newPassword;


    @NotBlank(message = "电话号码不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    private String newPhoneNumber;
}
