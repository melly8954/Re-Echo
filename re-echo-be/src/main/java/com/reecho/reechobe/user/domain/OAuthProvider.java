package com.reecho.reechobe.user.domain;

// Re-Echo가 지원하는 소셜 로그인 공급자를 표현한다.
public enum OAuthProvider {
    GOOGLE,
    KAKAO,
    GITHUB;

    // Spring Security OAuth2 registrationId를 내부 공급자 enum으로 변환한다.
    public static OAuthProvider fromRegistrationId(String registrationId) {
        if (registrationId == null || registrationId.isBlank()) {
            throw new IllegalArgumentException("OAuth 공급자는 필수입니다.");
        }

        try {
            return OAuthProvider.valueOf(registrationId.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("지원하지 않는 OAuth 공급자입니다.");
        }
    }
}
