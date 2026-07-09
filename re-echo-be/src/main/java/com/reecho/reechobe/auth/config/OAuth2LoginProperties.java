package com.reecho.reechobe.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// OAuth2 로그인 완료 후 클라이언트 전달 방식을 관리한다.
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "reecho.auth.oauth2-login")
public class OAuth2LoginProperties {

    private String refreshTokenCookieName;
    private boolean refreshTokenSecure;
    private String successRedirectUri;
    private String failureRedirectUri;
}
