package com.whu.medicalbackend.user.dto;


import com.whu.medicalbackend.user.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserVO {
    private long            id;
    private String          username;
    private String          password;
    private String          nickname;
    private String          phoneNumber;
    private LocalDateTime   createTime;
    private String          accessToken;
    private String          refreshToken;

    private UserVO(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.password = builder.password;
        this.nickname = builder.nickname;
        this.phoneNumber = builder.phoneNumber;
        this.createTime = builder.createTime;
        this.accessToken = builder.accessToken;
        this.refreshToken = builder.refreshToken;
    }

    public static class Builder {
        private User user;
        private long            id;
        private String          username;
        private String          password;
        private String          nickname;
        private String          phoneNumber;
        private LocalDateTime   createTime;
        private String          accessToken;
        private String          refreshToken;

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
        public Builder setPhoneNumber() {
            this.phoneNumber = user.getPhoneNumber();
            return this;
        }
        public Builder setCreateTime() {
            this.createTime = user.getCreateTime();
            return this;
        }
        public Builder setAccessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }
        public Builder setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public UserVO build(User user, String accessToken, String refreshToken) {
            this.setAccessToken(accessToken)
                    .setRefreshToken(refreshToken);
            return build(user);
        }

        public UserVO build(User user) {
            this.setUser(user).setId().setUsername().setPassword().setNickname().setPhoneNumber().setCreateTime();
            return build();
        }

        public UserVO build() {
            return new UserVO(this);
        }
    }
}
