package com.whu.medicalbackend.dto;


import com.whu.medicalbackend.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserVO {
    private long            id;
    private String          username;
    private String          password;
    private String          nickname;
    private LocalDateTime   createTime;

    private UserVO(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.password = builder.password;
        this.nickname = builder.nickname;
        this.createTime = builder.createTime;
    }

    public static class Builder {
        private User user;
        private long            id;
        private String          username;
        private String          password;
        private String          nickname;
        private LocalDateTime   createTime;

        public Builder setUser(User user) {
            this.user = user;
            return this;
        }

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
            return new UserVO(this.setUser(user).setId().setUsername().setPassword().setNickname().setCreateTime());
        }
    }
}
