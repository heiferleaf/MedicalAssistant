package com.whu.medicalbackend.family.dto;

import com.whu.medicalbackend.family.entity.FamilyMember;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FamilyMemberVO{
    private Long   userId;
    private String userName;
    private String userNickname;
    private String role;    // leader / normal
    private String status;  // active

    private FamilyMemberVO(FamilyMemberVOBuilder builder) {
        this.userId = builder.userId;
        this.userName = builder.userName;
        this.userNickname = builder.userNickname;
        this.role = builder.role;
        this.status = builder.status;
    }

    static public class FamilyMemberVOBuilder {
        private Long   userId;
        private String userName;
        private String userNickname;
        private String role;
        private String status;

        public FamilyMemberVOBuilder setUserId(Long userId) {
            this.userId = userId;
            return this;
        }

        public FamilyMemberVOBuilder setUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public FamilyMemberVOBuilder setUserNickname(String userNickname) {
            this.userNickname = userNickname;
            return this;
        }

        public FamilyMemberVOBuilder setRole(String role) {
            this.role = role;
            return this;
        }

        public FamilyMemberVOBuilder setStatus(String status) {
            this.status = status;
            return this;
        }

        public FamilyMemberVOBuilder setFamilyMember(FamilyMember familyMember) {
            return this.setUserId(familyMember.getUserId())
                    .setRole(familyMember.getRole())
                    .setStatus(familyMember.getStatus());
        }

        public FamilyMemberVO build() {
            return new FamilyMemberVO(this);
        }
    }
}
