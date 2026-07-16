package com.reecho.reechobe.member.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

// 이미지 필드의 생략과 null을 구분해 워크스페이스 프로필 PATCH 의미를 보존한다.
public class UpdateWorkspaceProfileRequest {

    @NotBlank
    @Size(max = 80)
    private String displayName;

    private UUID profileImageFileId;
    private boolean profileImageFileIdPresent;

    public String displayName() {
        return displayName;
    }

    public UUID profileImageFileId() {
        return profileImageFileId;
    }

    public boolean profileImageFileIdPresent() {
        return profileImageFileIdPresent;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @JsonSetter("profileImageFileId")
    public void setProfileImageFileId(UUID profileImageFileId) {
        this.profileImageFileId = profileImageFileId;
        this.profileImageFileIdPresent = true;
    }
}
