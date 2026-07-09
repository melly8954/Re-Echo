package com.reecho.reechobe.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class ApiAuthenticationEntryPointTest {

    @Test
    void 인증_실패를_공통_응답_형식으로_작성한다() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                request,
                response,
                new BadCredentialsException("인증 실패")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("errorCode").asText()).isEqualTo("AUTH_UNAUTHORIZED");
        assertThat(body.get("message").asText()).isEqualTo("인증이 필요합니다.");
        assertThat(body.get("result").isNull()).isTrue();
    }
}
