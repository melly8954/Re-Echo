package com.reecho.reechobe.user.service.command;

import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.common.exception.BusinessException;
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

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_UNAUTHORIZED));
        user.updateProfile(request.displayName(), request.profileImageUrl());
        return UserResponse.from(user);
    }
}
