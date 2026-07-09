package com.reecho.reechobe.auth.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// Re-Echo JWT 발급 설정을 관리한다.
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "reecho.auth.jwt")
public class JwtProperties {

    private String issuer;
    private String secret;
    private Duration accessTokenTtl;
    private Duration refreshTokenTtl;
}
