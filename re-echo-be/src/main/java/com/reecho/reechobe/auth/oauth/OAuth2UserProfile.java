package com.reecho.reechobe.auth.oauth;

import com.reecho.reechobe.user.domain.OAuthProvider;

// OAuth2 인증 결과에서 내부 계정 연결에 필요한 식별 정보를 표현한다.
public record OAuth2UserProfile(
        OAuthProvider provider,
        String providerUserId,
        String providerEmail
) {

    public OAuth2UserProfile {
        if (provider == null) {
            throw new IllegalArgumentException("OAuth 공급자는 필수입니다.");
        }

        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("OAuth 사용자 식별자는 필수입니다.");
        }
    }
}
