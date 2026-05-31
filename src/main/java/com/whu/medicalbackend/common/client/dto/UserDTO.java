package com.whu.medicalbackend.common.client.dto;

<<<<<<< HEAD
import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String phone;
=======
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户信息 DTO（用于服务间传输）
 *
 * @author Medical Assistant Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像 URL
     */
    private String avatar;

    /**
     * 性别（0-未知，1-男，2-女）
     */
    private Integer gender;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;
>>>>>>> fc315ac1946580259e7f744c73e93a43411327db
}
