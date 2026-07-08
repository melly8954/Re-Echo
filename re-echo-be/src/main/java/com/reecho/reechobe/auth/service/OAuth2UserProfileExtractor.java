package com.reecho.reechobe.auth.service;

import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.user.domain.OAuthProvider;
import java.util.Map;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

// OAuth2 인증 principal에서 공급자별 사용자 식별 정보를 추출한다.
@Component
public class OAuth2UserProfileExtractor {

    // OAuth2 인증 결과를 내부 사용자 연결용 프로필로 변환한다.
    public OAuth2UserProfile extract(OAuth2AuthenticationToken authentication) {
        OAuthProvider provider = OAuthProvider.fromRegistrationId(authentication.getAuthorizedClientRegistrationId());
        OAuth2User principal = authentication.getPrincipal();
        Map<String, Object> attributes = principal.getAttributes();

        return switch (provider) {
            case GOOGLE -> new OAuth2UserProfile(
                    provider,
                    principal.getName(),
                    asString(attributes.get("email"))
            );
            case GITHUB -> new OAuth2UserProfile(
                    provider,
                    principal.getName(),
                    asString(attributes.get("email"))
            );
            case KAKAO -> new OAuth2UserProfile(
                    provider,
                    principal.getName(),
                    kakaoEmail(attributes)
            );
        };
    }

    @SuppressWarnings("unchecked")
    private String kakaoEmail(Map<String, Object> attributes) {
        String oidcEmail = asString(attributes.get("email"));
        if (oidcEmail != null) {
            return oidcEmail;
        }

        Object kakaoAccount = attributes.get("kakao_account");
        if (!(kakaoAccount instanceof Map<?, ?> account)) {
            return null;
        }

        return asString(((Map<String, Object>) account).get("email"));
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }

        String text = String.valueOf(value);
        if (text.isBlank()) {
            throw new BusinessException(AuthErrorCode.AUTH_OAUTH_AUTHENTICATION_FAILED);
        }

        return text;
    }
}
