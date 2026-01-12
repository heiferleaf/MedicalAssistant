package com.whu.medicalbackend.dto;


import com.whu.medicalbackend.entity.User;

import java.time.LocalDateTime;


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
