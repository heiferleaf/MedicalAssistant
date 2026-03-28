package com.whu.medicalbackend.family.dto;

import com.whu.medicalbackend.family.entity.FamilyGroup;
import lombok.Data;

@Data
public class FamilyCreateVO {
    private Long    groupId;
    private String  groupName;
    private String  ownerUserNickname;
    private String  description;

    private FamilyCreateVO(FamilyCreateVOBuilder builder) {
        this.groupId = builder.groupId;
        this.groupName = builder.groupName;
        this.ownerUserNickname = builder.ownerUserNickname;
        this.description = builder.description;
    }

    public static class FamilyCreateVOBuilder {
        private FamilyGroup familyGroup;
        private Long        groupId;
        private String      groupName;
        private String      ownerUserNickname;
        private String      description;

        public FamilyCreateVOBuilder fromFamilyGroup(FamilyGroup familyGroup) {
            this.familyGroup     = familyGroup;
            this.groupId         = familyGroup.getId();
            this.groupName       = familyGroup.getGroupName();
            this.description     = familyGroup.getDescription();
            return this;
        }

        public FamilyCreateVOBuilder setGroupId(Long groupId) {
            this.groupId         = groupId;
            return this;
        }

        public FamilyCreateVOBuilder setGroupName(String groupName) {
            this.groupName       = groupName;
            return this;
        }

        public FamilyCreateVOBuilder setOwnerUserNickname(String ownerUserNickname) {
            this.ownerUserNickname = ownerUserNickname;
            return this;
        }

        public FamilyCreateVOBuilder setDescription(String description) {
            this.description      = description;
            return this;
        }

        public FamilyCreateVO build() {
            return new FamilyCreateVO(this);
        }
    }
}
