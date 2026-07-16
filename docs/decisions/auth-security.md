# Re-Echo Decision Records: 인증과 보안


### Status

Accepted

### Context

Re-Echo는 소셜 로그인 기반 인증을 제공하고, REST API와 WebSocket
handshake에서 인증 상태를 검증해야 한다.

### Decision

Access Token은 로그인과 refresh 응답 body로 반환하고 브라우저 메모리에
보관한다.
Refresh Token은 `HttpOnly`, `Secure`, `SameSite=Lax` cookie로만
전달한다.
WebSocket handshake는 Access Token 기반 인증을 사용한다.

### Reason

PRD와 API 문서가 Access Token과 Refresh Token 전달 방식을 확정한다.
Architecture와 Coding Convention은 WebSocket handshake도 HTTP 인증과
같은 Access Token 기준을 따른다고 명시한다.

### Consequences

- Access Token은 응답 header로 전달하지 않는다.
- Refresh Token은 JavaScript에서 직접 접근할 수 없는 cookie 정책을
  따른다.
- WebSocket 연결 전 인증 처리와 REST 인증 처리는 같은 토큰 기준을
  공유한다.
- 토큰, 쿠키, Authorization header는 로그에 남기지 않는다.

### Source

- `docs/prd.md`
- `docs/Architecture.md`
- `docs/API.md`
- `docs/coding-convention.md`
