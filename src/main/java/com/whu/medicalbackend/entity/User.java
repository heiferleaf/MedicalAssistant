package com.whu.medicalbackend.entity;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

// @Data = @Getter + @Setter + @RequiredArgsConstructor + @ToString + @EqualsAndHashCode
// 但不只用 @Data，因为：
// 1. 需要自定义 equals/hashCode（只基于 username）
// 2. 需要无参构造（JPA/Hibernate 要求）
@Data
@NoArgsConstructor // 生成无参构造函数（必须！用于 ORM 反射）
@AllArgsConstructor // 可选：生成全参构造（方便测试）
public class User implements Serializable {

    private static final long serialVersionUID = 1L; // 注意拼写：serialVersionUID

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 自定义构造函数（带 username + password）
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    // 自定义构造函数（带 username + password + nickname）
    public User(String username, String password, String nickname) {
        this(username, password); // 复用上一个构造函数
        this.nickname = nickname;
    }

    // 自定义 equals 和 hashCode：仅基于 username
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}