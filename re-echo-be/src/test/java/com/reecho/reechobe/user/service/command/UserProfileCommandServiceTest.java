package com.reecho.reechobe.user.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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

    private UserProfileCommandService service;

    @BeforeEach
    void setUp() {
        service = new UserProfileCommandService(userRepository);
    }

    @Test
    void 계정_기본_프로필을_수정한다() {
        UUID userId = UUID.randomUUID();
        User user = User.createActive("기존 이름", null);
        ReflectionTestUtils.setField(user, "id", userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "새 이름",
                "https://example.com/profile.png"
        );

        UserResponse result = service.updateProfile(userId, request);

        assertThat(result.displayName()).isEqualTo("새 이름");
        assertThat(result.profileImageUrl()).isEqualTo("https://example.com/profile.png");
    }
}
