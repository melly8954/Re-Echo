package com.reecho.reechobe.auth.oauth;

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
            case GOOGLE -> profile(
                    provider,
                    principal.getName(),
                    asString(attributes.get("email")),
                    asString(attributes.get("name")),
                    asString(attributes.get("picture"))
            );
            case GITHUB -> profile(
                    provider,
                    principal.getName(),
                    asString(attributes.get("email")),
                    firstNonBlank(
                            asString(attributes.get("name")),
                            asString(attributes.get("login"))
                    ),
                    asString(attributes.get("avatar_url"))
            );
            case KAKAO -> profile(
                    provider,
                    principal.getName(),
                    kakaoEmail(attributes),
                    kakaoProfileValue(attributes, "nickname"),
                    firstNonBlank(
                            asString(attributes.get("picture")),
                            kakaoProfileValue(attributes, "profile_image_url"),
                            kakaoProfileValue(attributes, "profile_image")
                    )
            );
        };
    }

    private OAuth2UserProfile profile(
            OAuthProvider provider,
            String providerUserId,
            String providerEmail,
            String displayName,
            String profileImageUrl
    ) {
        String resolvedDisplayName = firstNonBlank(
                displayName,
                emailLocalPart(providerEmail),
                providerUserId
        );
        if (resolvedDisplayName == null) {
            throw new IllegalArgumentException("OAuth 사용자 표시 이름을 확인할 수 없습니다.");
        }
        if (resolvedDisplayName.length() > 80) {
            resolvedDisplayName = resolvedDisplayName.substring(0, 80);
        }
        return new OAuth2UserProfile(
                provider,
                providerUserId,
                providerEmail,
                resolvedDisplayName,
                profileImageUrl
        );
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

    @SuppressWarnings("unchecked")
    private String kakaoProfileValue(Map<String, Object> attributes, String key) {
        String oidcValue = asString(attributes.get(key));
        if (oidcValue != null) {
            return oidcValue;
        }

        Object properties = attributes.get("properties");
        if (properties instanceof Map<?, ?> propertyMap) {
            String value = asString(((Map<String, Object>) propertyMap).get(key));
            if (value != null) {
                return value;
            }
        }

        Object kakaoAccount = attributes.get("kakao_account");
        if (!(kakaoAccount instanceof Map<?, ?> account)) {
            return null;
        }
        Object profile = ((Map<String, Object>) account).get("profile");
        if (!(profile instanceof Map<?, ?> profileMap)) {
            return null;
        }
        return asString(((Map<String, Object>) profileMap).get(key));
    }

    private String emailLocalPart(String email) {
        if (email == null) {
            return null;
        }
        int separatorIndex = email.indexOf('@');
        return separatorIndex > 0 ? email.substring(0, separatorIndex) : email;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }

        String text = String.valueOf(value);
        if (text.isBlank()) {
            return null;
        }

        return text;
    }
}
