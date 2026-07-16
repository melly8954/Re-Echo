# Re-Echo 1차 MVP Architecture: 배포와 운영

## 11. Deployment Architecture

배포 구조는 다음과 같다.

- 프론트엔드는 정적 파일을 CDN으로 서빙한다.
- 백엔드 요청은 Reverse Proxy 또는 Load Balancer를 통해 Spring Boot 애플리케이션으로 전달한다.
- 백엔드는 PostgreSQL, Redis, Cloudflare R2와 연동한다.

### 11.1 초기 운영

- 앱 인스턴스 1개로 시작
- 구조는 멀티 인스턴스 확장이 가능하도록 설계
- 로컬 개발 또는 단순 운영 환경에서는 PostgreSQL과 Redis를 docker-compose로 띄울 수 있다.

### 11.2 확장 원칙

- 백엔드는 수평 확장 가능
- Redis Pub/Sub 및 공유 상태로 멀티 인스턴스를 지원

## 12. Technology Decisions

- Backend
  - Java
  - Spring Boot
- Frontend
  - React
  - Vite
- Rendering
  - CSR SPA
- RDB
  - PostgreSQL
- Shared State / Realtime Support
  - Redis
- Object Storage
  - Cloudflare R2
- Scheduler
  - 애플리케이션 내부 스케줄러

## 13. Non-functional Requirements

### 13.1 우선순위

1. 실시간 안정성과 장애 복구 용이성
2. 운영 단순성과 개발 속도

### 13.2 관측성

- 애플리케이션 로그
- 메트릭
- 에러 추적
- 기본 대시보드

예상 구성:

- Spring Boot Actuator
- Micrometer
- Prometheus
- Grafana
- Sentry 또는 동급 에러 추적 도구

### 13.3 운영 원칙

- Redis 장애 시 실시간 기능 저하는 허용
- 영속 API는 가능한 한 유지
- 정기 작업은 내부 스케줄러로 단순하게 운영

## 14. Design Decisions

- 단일 모놀리식 백엔드로 시작
- 프론트엔드와 백엔드는 분리 배포
- 프론트는 CSR SPA
- Access/Refresh Token 기반 인증
- OAuth 로그인은 백엔드 주도 Spring Security `oauth2Login` 흐름을 사용
- OAuth 임시 상태는 Redis에 저장
- WebSocket은 Access Token handshake 사용
- Redis Pub/Sub로 멀티 인스턴스 실시간 이벤트 전파
- PostgreSQL을 최종 진실 소스로 유지
- Redis는 보조 저장소 및 분산 상태 계층으로 사용
- 계정 기본 프로필은 사용자 계정에 저장한다.
- 워크스페이스에서 사용하는 닉네임과 프로필 이미지는 생성·참여 시
  계정 기본 프로필을 복사한 뒤 워크스페이스 멤버십에 귀속한다.
- 파일 저장소는 스토리지 인터페이스 + R2 어댑터 구조로 구성
- 보관된 워크스페이스와 채널은 복원 전까지 읽기 전용으로 유지한다
- 정리 배치는 내부 스케줄러 사용
