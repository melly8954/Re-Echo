package com.reecho.reechobe.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.reecho.reechobe.user.domain.OAuthProvider;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

class OAuth2UserProfileExtractorTest {

    private final OAuth2UserProfileExtractor extractor = new OAuth2UserProfileExtractor();

    @Test
    void google_principal에서_사용자_식별자를_추출한다() {
        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(),
                Map.of(
                        "sub", "google-user-id",
                        "email", "user@example.com"
                ),
                "sub"
        );
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "google"
        );

        OAuth2UserProfile profile = extractor.extract(authentication);

        assertThat(profile.provider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(profile.providerUserId()).isEqualTo("google-user-id");
        assertThat(profile.providerEmail()).isEqualTo("user@example.com");
    }

    @Test
    void kakao_principal에서_중첩된_이메일을_추출한다() {
        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(),
                Map.of(
                        "id", "kakao-user-id",
                        "kakao_account", Map.of("email", "user@example.com")
                ),
                "id"
        );
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "kakao"
        );

        OAuth2UserProfile profile = extractor.extract(authentication);

        assertThat(profile.provider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(profile.providerUserId()).isEqualTo("kakao-user-id");
        assertThat(profile.providerEmail()).isEqualTo("user@example.com");
    }

    @Test
    void kakao_oidc_principal에서_email_claim을_우선_사용한다() {
        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(),
                Map.of(
                        "sub", "kakao-sub-id",
                        "email", "oidc@example.com",
                        "kakao_account", Map.of("email", "legacy@example.com")
                ),
                "sub"
        );
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "kakao"
        );

        OAuth2UserProfile profile = extractor.extract(authentication);

        assertThat(profile.provider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(profile.providerUserId()).isEqualTo("kakao-sub-id");
        assertThat(profile.providerEmail()).isEqualTo("oidc@example.com");
    }
}
