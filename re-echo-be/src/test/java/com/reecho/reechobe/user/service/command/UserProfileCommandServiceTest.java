package com.reecho.reechobe.user.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reecho.reechobe.file.service.ProfileImageFileService;
import com.reecho.reechobe.user.domain.User;
import com.reecho.reechobe.user.dto.UpdateUserProfileRequest;
import com.reecho.reechobe.user.dto.UserResponse;
import com.reecho.reechobe.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserProfileCommandServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileImageFileService profileImageFileService;

    private UserProfileCommandService service;

    @BeforeEach
    void setUp() {
        service = new UserProfileCommandService(userRepository, profileImageFileService);
    }

    @Test
    void 계정_기본_표시_이름을_수정한다() {
        UUID userId = UUID.randomUUID();
        User user = User.createActive("기존 이름", "https://example.com/profile.png");
        ReflectionTestUtils.setField(user, "id", userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setDisplayName("새 이름");

        UserResponse result = service.updateProfile(userId, request);

        assertThat(result.displayName()).isEqualTo("새 이름");
        assertThat(result.profileImageUrl()).isEqualTo("https://example.com/profile.png");
        verifyNoInteractions(profileImageFileService);
    }

    @Test
    void 업로드된_프로필_이미지로_수정한다() {
        UUID userId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        User user = User.createActive("기존 이름", null);
        ReflectionTestUtils.setField(user, "id", userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(profileImageFileService.requireUploadedAccountProfileImageUrl(userId, fileId))
                .thenReturn("https://cdn.example.com/profile.png");
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setDisplayName("새 이름");
        request.setProfileImageFileId(fileId);

        UserResponse result = service.updateProfile(userId, request);

        assertThat(result.displayName()).isEqualTo("새 이름");
        assertThat(result.profileImageUrl()).isEqualTo("https://cdn.example.com/profile.png");
    }
}
