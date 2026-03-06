package com.whu.medicalbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Data
public class FamilyDetailVO {
    private GroupInfo group;
    private List<FamilyMemberVO> members;


    @Data
    @NoArgsConstructor
    public static class GroupInfo {
        private Long groupId;
        private String groupName;
        private String description;
        private String ownerUserNickname;

        public GroupInfo(Long groupId, String groupName, String description, String ownerUserNickname) {
            this.groupId = groupId;
            this.groupName = groupName;
            this.description = description;
            this.ownerUserNickname = ownerUserNickname;
        }
    }


}