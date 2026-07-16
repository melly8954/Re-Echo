# Re-Echo 1차 MVP Architecture: 데이터와 실시간 처리


### 6.1 PostgreSQL

최종 진실 소스로 다음 데이터를 저장한다.

- 사용자 계정과 계정 기본 프로필
- 워크스페이스
- 워크스페이스 멤버십
- 워크스페이스별 닉네임 및 프로필 이미지
- 채널
- 메시지
- 읽음 상태
- 파일 메타데이터
- 강제 삭제 이력

### 6.2 Redis

보조 저장소 및 분산 상태 저장소로 사용한다.

- `jti` 기반 세션별 Refresh Token 상태와 TTL 관리
- Spring Session 기반 OAuth 로그인 중간 상태 공유
- Pub/Sub
- 입력 중 상태
- 인스턴스 간 공유 상태
- 일부 보조 운영 데이터

원칙:

- 최종 진실 소스는 PostgreSQL이다.
- Redis는 캐시, 보조 상태, 분산 이벤트 전파 역할만 맡는다.

### 6.3 단일 RDB 원칙

- 사용자 권한 데이터와 메시지 데이터는 하나의 PostgreSQL에서 함께 운영한다.
- 워크스페이스별 프로필 데이터도 멤버십 데이터와 같은 RDB에서 함께 운영한다.
- MVP 단계에서는 DB 분리보다 단일 운영의 단순성을 우선한다.

## 7. Real-time / Async Strategy

### 7.1 실시간 처리 범위

- 메시지 송수신
- 입력 중 표시

### 7.2 WebSocket 인증

- Access Token 기반 handshake 인증

### 7.3 멀티 인스턴스 이벤트 전파

- Redis Pub/Sub 사용
- WebSocket 연결과 일반 HTTP 요청은 같은 백엔드 애플리케이션에서 처리한다.
- Sticky session은 필수 전제로 두지 않는다.

### 7.4 Redis 장애 시 완화 원칙

- 실시간 기능은 일시적으로 저하될 수 있다.
- 일반 API와 영속 데이터 처리는 최대한 유지한다.

## 8. File & Storage Strategy
