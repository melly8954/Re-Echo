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
