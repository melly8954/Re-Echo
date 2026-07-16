package com.reecho.reechobe.security;

import com.reecho.reechobe.auth.handler.OAuth2LoginFailureHandler;
import com.reecho.reechobe.auth.handler.OAuth2LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// HTTP 보안 필터 체인과 OAuth2 로그인 흐름을 구성한다.
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oauth2LoginFailureHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;

    // OAuth 로그인 요청과 콜백에서만 Redis 공유 HttpSession을 허용한다.
    @Bean
    @Order(1)
    public SecurityFilterChain oauthSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/oauth2/**", "/login/oauth2/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oauth2LoginSuccessHandler)
                        .failureHandler(oauth2LoginFailureHandler)
                )
                .build();
    }

    // OAuth 경로 외 요청은 세션 없이 Access Token으로만 인증한다.
    @Bean
    @Order(2)
    public SecurityFilterChain webSocketSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/ws", "/ws/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    // 일반 API 요청은 세션 없이 Access Token으로만 인증한다.
    @Bean
    @Order(3)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/invite-links/*").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
