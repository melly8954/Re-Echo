package com.reecho.reechobe.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.member.exception.MemberErrorCode;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void 계정_기본_프로필로_활성_사용자를_생성한다() {
        User user = User.createActive("  사용자  ", "https://example.com/profile.png");

        assertThat(user.getDisplayName()).isEqualTo("사용자");
        assertThat(user.getProfileImageUrl()).isEqualTo("https://example.com/profile.png");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void 표시_이름이_비어있으면_사용자를_생성할_수_없다() {
        assertThatThrownBy(() -> User.createActive(" ", null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MemberErrorCode.MEMBER_INVALID_DISPLAY_NAME);
    }

    @Test
    void 표시_이름이_80자를_초과하면_사용자를_생성할_수_없다() {
        assertThatThrownBy(() -> User.createActive("가".repeat(81), null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MemberErrorCode.MEMBER_INVALID_DISPLAY_NAME);
    }

    @Test
    void 계정_기본_프로필을_수정한다() {
        User user = User.createActive("기존 이름", null);

        user.updateDisplayName("새 이름");
        user.updateProfileImage("https://example.com/new-profile.png", null);

        assertThat(user.getDisplayName()).isEqualTo("새 이름");
        assertThat(user.getProfileImageUrl())
                .isEqualTo("https://example.com/new-profile.png");
    }
}
