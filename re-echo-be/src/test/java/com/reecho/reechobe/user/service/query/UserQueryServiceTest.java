package com.reecho.reechobe.user.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.user.domain.User;
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
class UserQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserQueryService service;

    @BeforeEach
    void setUp() {
        service = new UserQueryService(userRepository);
    }

    @Test
    void 인증_사용자의_계정_기본_프로필을_조회한다() {
        UUID userId = UUID.randomUUID();
        User user = User.createActive("사용자", "https://example.com/profile.png");
        ReflectionTestUtils.setField(user, "id", userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse result = service.getCurrentUser(userId);

        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.displayName()).isEqualTo("사용자");
        assertThat(result.profileImageUrl()).isEqualTo("https://example.com/profile.png");
    }

    @Test
    void 인증_사용자가_없으면_인증_예외를_던진다() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentUser(userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTH_UNAUTHORIZED);
    }
}
