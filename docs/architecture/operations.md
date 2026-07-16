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

#### 13.2.1 상태 확인과 접근 범위

- Reverse Proxy 또는 Load Balancer는 Actuator의 liveness와 readiness
  상태를 사용해 인스턴스의 트래픽 수용 가능 여부를 판단한다.
- readiness는 PostgreSQL 연결 가능 여부를 포함한다.
- Redis 장애는 실시간 기능 저하로 분리하므로 readiness 실패의 원인으로
  사용하지 않는다. Redis 상태는 메트릭과 경고로 관찰한다.
- Prometheus는 내부 운영 네트워크에서만
  `/actuator/prometheus`를 수집한다. 해당 엔드포인트를 인터넷에
  공개하지 않는다.
- health 응답에는 인증 정보, 연결 문자열, 외부 서비스 상세 오류를
  포함하지 않는다.

#### 13.2.2 메트릭과 대시보드

- 기본 메트릭은 HTTP 요청량·오류율·응답 시간, JVM·GC·CPU,
  PostgreSQL 연결 풀, Redis 상태, WebSocket 연결 수, 정기 작업
  성공·실패·실행 시간을 포함한다.
- Grafana 기본 대시보드는 HTTP 5xx 비율과 p95 응답 시간,
  PostgreSQL 연결 풀, JVM 상태, Redis 상태, 정기 작업 결과를
  한 화면에서 확인할 수 있어야 한다.
- 메트릭 태그에는 `userId`, `workspaceId`, `channelId`, 이메일,
  메시지 ID처럼 값 종류가 계속 늘어나는 식별자를 사용하지 않는다.
- 상태 확인·오류율·응답 시간 등 장애 판단에 필요한 최소 메트릭부터
  도입하고, 업무 메트릭은 실제 운영 필요가 확인된 경우에만 추가한다.

#### 13.2.3 로그와 오류 추적

- 운영 로그는 요청 추적 ID, HTTP 메서드와 경로, 응답 상태,
  처리 시간, 예외 종류를 구조화하여 기록한다.
- Access Token, Refresh Token, OAuth 인가 코드, 비밀번호, 메시지
  본문 등 인증 정보와 사용자 민감 정보는 로그와 오류 추적 도구에
  기록하지 않는다.
- 오류 추적 도구는 운영 환경의 처리되지 않은 예외부터 수집한다.
  Sentry 연동 여부와 알림 수신 채널은 실제 운영 환경을 구성할 때
  확정한다.

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
