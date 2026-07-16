# Re-Echo Decision Records: 제품과 기반 구조

## Decision 001. 1차 MVP 범위를 채널 기반 SaaS 협업으로 제한

### Status

Accepted

### Context

Re-Echo 1차 MVP는 여러 조직이 사용하는 SaaS형 협업 서비스다.
제품 목표는 워크스페이스 생성, 멤버 초대, 채널 기반 실시간 대화,
파일 공유까지의 기본 협업 경험을 데모 가능 수준으로 완성하는 것이다.

### Decision

1차 MVP 범위는 채널 기반 SaaS 협업 흐름으로 제한한다.
DM, 메시지 검색, 멘션, AI, 음성, 화상, 푸시 알림은 MVP 범위에서
제외하고 후속 확장 후보로 둔다.

### Reason

문서에 직접 명시된 이유는 아니지만, 다음 근거를 바탕으로 추론한다.

- PRD는 복잡한 AI/음성/화상 기능보다 채널 중심 협업의 기본 흐름을
  빠르게 제공하는 데 집중한다고 정의한다.
- Architecture와 API 문서는 제외 범위를 반복해서 명시한다.
- ERD는 제외 범위에 해당하는 도메인 테이블을 포함하지 않는다.

### Consequences

- 초기 구현 범위가 워크스페이스, 채널, 메시지, 파일 공유 중심으로
  명확해진다.
- 후속 확장 기능은 현재 API, ERD, 코딩 컨벤션에 임의로 추가하지
  않아야 한다.
- DM, 검색, 멘션, AI, 음성, 화상, 푸시 알림 관련 테이블과 API는
  1차 MVP 산출물에서 제외된다.

### Source

- `docs/prd.md`
- `docs/Architecture.md`
- `docs/ERD.md`
- `docs/API.md`

## Decision 002. Java Spring Boot 모놀리식 백엔드와 React Vite CSR SPA 선택

### Status

Accepted

### Context

1차 MVP는 실시간 채팅과 파일 공유 흐름을 안정적으로 제공해야 하며,
초기에는 운영 단순성과 개발 속도가 중요하다.

### Decision

백엔드는 `Java + Spring Boot` 기반 단일 모놀리식으로 시작한다.
프론트엔드는 `React + Vite` 기반 `CSR SPA`로 구성하고, 프론트엔드와
백엔드는 분리 배포한다.

### Reason

문서에 직접 명시된 이유는 아니지만, 다음 근거를 바탕으로 추론한다.

- Architecture는 복잡한 확장보다 핵심 사용자 흐름의 안정적 제공을
  이번 아키텍처의 목표로 둔다.
- 비기능 요구사항은 실시간 안정성, 장애 복구 용이성, 운영 단순성,
  개발 속도를 우선순위로 둔다.
- Coding Convention은 Architecture에 없는 패턴을 임의로 도입하지
  않도록 제한한다.

### Consequences

- 초기에는 서비스 분리보다 단일 백엔드 안에서 도메인 계층을 나누는
  방식으로 구현한다.
- 프론트엔드는 CDN 정적 배포가 가능하다.
- 백엔드 수평 확장은 가능하게 두되, 마이크로서비스 분리는 MVP의
  기본 전제가 아니다.

### Source

- `docs/Architecture.md`
- `docs/coding-convention.md`

## Decision 003. PostgreSQL을 최종 진실 소스로 사용

### Status

Accepted

### Context

Re-Echo는 사용자, 워크스페이스, 멤버십, 채널, 메시지, 읽음 상태,
파일 메타데이터처럼 관계와 권한 검증이 중요한 데이터를 관리한다.

### Decision

영속 데이터의 최종 진실 소스는 PostgreSQL로 둔다.
사용자 권한 데이터, 메시지 데이터, 워크스페이스별 프로필 데이터는
MVP 단계에서 하나의 PostgreSQL에서 함께 운영한다.

### Reason

Architecture와 ERD가 PostgreSQL을 최종 진실 소스로 명시한다.
MVP 단계에서는 DB 분리보다 단일 운영의 단순성을 우선한다고
Architecture에 명시되어 있다.

### Consequences

- 권한, 메시지, 파일 메타데이터 등 핵심 상태는 PostgreSQL 기준으로
  판단한다.
- Redis나 외부 스토리지를 영속 비즈니스 데이터의 기준으로 삼지 않는다.
- 데이터 모델 변경은 ERD와 마이그레이션 정책을 함께 따른다.

### Source

- `docs/Architecture.md`
- `docs/ERD.md`

## Decision 004. Redis를 보조 상태와 실시간 전파 계층으로 제한

### Status

Accepted

### Context

Re-Echo는 WebSocket 기반 실시간 메시지 전파와 OAuth 로그인 중간 상태,
토큰 보조 관리, 입력 중 상태를 처리해야 한다.

### Decision

Redis는 캐시, 보조 상태, OAuth 로그인 중간 상태, 토큰 보조 관리,
입력 중 상태, Pub/Sub 기반 이벤트 전파에 사용한다.
최종 진실 소스로는 사용하지 않는다.

### Reason

Architecture는 Redis를 보조 저장소 및 분산 상태 저장소로 정의하고,
최종 진실 소스는 PostgreSQL이라고 명시한다.
ERD도 Redis를 실시간 상태와 보조 운영 데이터 저장소로 분리하고
ERD 범위에는 포함하지 않는다.

### Consequences

- Redis 장애 시 실시간 기능은 일시적으로 저하될 수 있지만, 일반 API와
  영속 데이터 처리는 가능한 한 유지해야 한다.
- Redis 데이터만으로 영구 권한, 메시지, 파일 상태를 판단하지 않는다.
- 멀티 인스턴스 실시간 이벤트 전파는 Redis Pub/Sub를 기준으로 구현한다.

### Source

- `docs/Architecture.md`
- `docs/ERD.md`

## Decision 005. Access Token과 Refresh Token 저장 방식을 분리
