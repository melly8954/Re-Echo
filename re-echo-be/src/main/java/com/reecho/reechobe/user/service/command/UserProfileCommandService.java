package com.reecho.reechobe.user.service.command;

import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.file.service.ProfileImageFileService;
import com.reecho.reechobe.user.domain.User;
import com.reecho.reechobe.user.dto.UpdateUserProfileRequest;
import com.reecho.reechobe.user.dto.UserResponse;
import com.reecho.reechobe.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 계정 기본 프로필 변경을 처리한다.
@Service
@RequiredArgsConstructor
public class UserProfileCommandService {

    private final UserRepository userRepository;
    private final ProfileImageFileService profileImageFileService;

    @Transactional
    // 계정 기본 프로필을 갱신하고 교체된 이미지 파일은 정리 대상으로 표시한다.
    public UserResponse updateProfile(UUID userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_UNAUTHORIZED));
        UUID previousProfileImageFileId = user.getProfileImageFileId();
        user.updateDisplayName(request.displayName());
        if (request.profileImageFileIdPresent()) {
            updateProfileImage(userId, user, previousProfileImageFileId, request);
        }
        return UserResponse.from(user);
    }

    // PATCH에서 이미지 필드가 전달된 경우에만 업로드 완료 검증과 교체 처리를 수행한다.
    private void updateProfileImage(
            UUID userId,
            User user,
            UUID previousProfileImageFileId,
            UpdateUserProfileRequest request
    ) {
        if (request.profileImageFileId() == null) {
            user.removeProfileImage();
            markPreviousProfileImageOrphaned(userId, previousProfileImageFileId, null);
            return;
        }
        String profileImageUrl = profileImageFileService.requireUploadedAccountProfileImageUrl(
                userId,
                request.profileImageFileId()
        );
        markPreviousProfileImageOrphaned(userId, previousProfileImageFileId, request.profileImageFileId());
        user.updateProfileImage(profileImageUrl, request.profileImageFileId());
    }

    // 새 이미지 반영 후에만 이전 파일을 고아 상태로 바꿔 롤백 실패 시 참조를 보존한다.
    private void markPreviousProfileImageOrphaned(
            UUID userId,
            UUID previousProfileImageFileId,
            UUID nextProfileImageFileId
    ) {
        if (previousProfileImageFileId == null || previousProfileImageFileId.equals(nextProfileImageFileId)) {
            return;
        }
        profileImageFileService.markAccountProfileImageOrphaned(userId, previousProfileImageFileId);
    }
}
