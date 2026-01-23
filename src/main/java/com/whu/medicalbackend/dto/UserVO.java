package com.whu.medicalbackend.dto;


import com.whu.medicalbackend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {
    private long            id;
    private String          username;
    private String          password;
    private String          nickname;
    private LocalDateTime   createTime;

    private UserVO(Builder builder) {

    }

    public static class Builder {
        public User user;
        public long            id;
        public String          username;
        public String          password;
        public String          nickname;
        public LocalDateTime   createTime;

        public void setUser(User user) {this.user = user;}

        public Builder setId() {
            this.id = user.getId();
            return this;
        }
        public Builder setUsername() {
            this.username = user.getUsername();
            return this;
        }
        public Builder setPassword() {
            this.password = user.getPassword();
            return this;
        }
        public Builder setNickname() {
            this.nickname = user.getNickname();
            return this;
        }

        public Builder setCreateTime() {
            this.createTime = user.getCreateTime();
            return this;
        }

        public UserVO build(User user) {
            setUser(user);
            return new UserVO(this.setId().setUsername().setPassword().setNickname().setCreateTime());
        }
    }
}
