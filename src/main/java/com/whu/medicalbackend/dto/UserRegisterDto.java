package com.whu.medicalbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当发送的请求被Controller截获，会按照注解校验传入的参数是否合法，如果不合法，
 * 会直接抛出 MethodArgumentNotValidException ， 被全局异常处理捕获
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterDto {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "用户名长度需要是6-20")
    @Pattern(regexp = "^[0-9a-zA-Z_]+$", message = "用户名只能包含大小写字母，数字、以及_")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "密码长度为6-20位")
    @Pattern(regexp = "^[0-9a-zA-Z_]+$", message = "密码只能包含大小写字母，数字、以及_")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "昵称长度为6-20位")
    @Pattern(regexp = "^[0-9a-zA-Z_]+$", message = "昵称只能包含大小写字母，数字、以及_")
    private String nickname;

    @NotBlank(message = "电话号码不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    private String phoneNumber;
}
