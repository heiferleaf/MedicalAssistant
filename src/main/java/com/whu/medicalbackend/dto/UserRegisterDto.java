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
    @Pattern(regexp = "^\\w{6,20}$", message = "用户名长度需要是6-20")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^. {6,20}$", message = "密码长度为6-20位")
    private String password;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^. {6,20}$", message = "昵称长度为6-20位")
    private String nickname;
}
