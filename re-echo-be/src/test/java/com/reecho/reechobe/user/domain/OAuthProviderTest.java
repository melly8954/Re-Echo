package com.reecho.reechobe.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OAuthProviderTest {

    @Test
    void registrationId를_OAuth_공급자로_변환한다() {
        assertThat(OAuthProvider.fromRegistrationId("google")).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(OAuthProvider.fromRegistrationId("kakao")).isEqualTo(OAuthProvider.KAKAO);
        assertThat(OAuthProvider.fromRegistrationId("github")).isEqualTo(OAuthProvider.GITHUB);
    }

    @Test
    void 지원하지_않는_registrationId면_예외를_던진다() {
        assertThatThrownBy(() -> OAuthProvider.fromRegistrationId("naver"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지원하지 않는 OAuth 공급자입니다.");
    }
}
