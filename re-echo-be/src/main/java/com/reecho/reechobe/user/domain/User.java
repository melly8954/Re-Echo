package com.reecho.reechobe.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Re-Echo 사용자 기본 계정을 표현한다.
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static User createActive(String displayName, String profileImageUrl) {
        String normalizedDisplayName = normalizeDisplayName(displayName);
        return User.builder()
                .displayName(normalizedDisplayName)
                .profileImageUrl(profileImageUrl)
                .status(UserStatus.ACTIVE)
                .build();
    }

    public void updateProfile(String displayName, String profileImageUrl) {
        this.displayName = normalizeDisplayName(displayName);
        this.profileImageUrl = profileImageUrl;
    }

    public void deactivate() {
        this.status = UserStatus.DEACTIVATED;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private static String normalizeDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("계정 표시 이름은 1자 이상 80자 이하여야 합니다.");
        }
        String normalizedDisplayName = displayName.trim();
        if (normalizedDisplayName.length() > 80) {
            throw new IllegalArgumentException("계정 표시 이름은 1자 이상 80자 이하여야 합니다.");
        }
        return normalizedDisplayName;
    }
}
