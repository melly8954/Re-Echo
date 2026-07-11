# Re-Echo Decision Records

## Decisions Overview

이 문서는 Re-Echo 1차 MVP 문서에 이미 확정된 주요 설계 결정을
ADR 형식으로 정리한다.

새로운 설계를 추가하지 않고, 기존 문서의 제품 범위, 아키텍처,
데이터 모델, API 계약, 코딩 컨벤션에서 확인되는 결정만 기록한다.

## Source Documents

- `docs/prd.md`
- `docs/Architecture.md`
- `docs/ERD.md`
- `docs/API.md`
- `docs/coding-convention.md`

## Decision Index

| Decision | Title | Status |
| --- | --- | --- |
| Decision 001 | 1차 MVP 범위를 채널 기반 SaaS 협업으로 제한 | Accepted |
| Decision 002 | Java Spring Boot 모놀리식 백엔드와 React Vite CSR SPA 선택 | Accepted |
| Decision 003 | PostgreSQL을 최종 진실 소스로 사용 | Accepted |
| Decision 004 | Redis를 보조 상태와 실시간 전파 계층으로 제한 | Accepted |
| Decision 005 | Access Token과 Refresh Token 저장 방식을 분리 | Accepted |
| Decision 006 | 워크스페이스 멤버십을 권한과 프로필의 기준으로 사용 | Accepted |
| Decision 007 | 채널은 기본 채널, 공개 채널, 비공개 채널 정책으로 운영 | Accepted |
| Decision 008 | 실시간 처리는 메시지와 입력 중 표시로 제한 | Accepted |
| Decision 009 | 메시지 목록은 cursor pagination으로 조회 | Accepted |
| Decision 010 | 읽음 상태는 채널별 마지막 읽은 메시지 기준으로 관리 | Accepted |
| Decision 011 | 파일은 R2 직접 전송과 DB 메타데이터로 분리 | Accepted |
| Decision 012 | 워크스페이스와 채널은 보관 후 만료 삭제 모델 사용 | Accepted |
| Decision 013 | REST API는 공통 응답 envelope과 에러 코드 체계를 사용 | Accepted |
| Decision 014 | DTO와 Entity를 분리하고 계층 책임을 명확히 유지 | Accepted |
| Decision 015 | Command Service와 Query Service를 책임 기준으로 분리 | Accepted |
| Decision 016 | 상태값은 문자열 enum 컬럼으로 시작 | Accepted |
| Decision 017 | Flyway 마이그레이션으로 DB 변경 이력을 관리 | Accepted |

## Decision Records

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

## Decision 006. 워크스페이스 멤버십을 권한과 프로필의 기준으로 사용

### Status

Accepted

### Context

사용자는 여러 워크스페이스에 참여할 수 있고, 워크스페이스마다 역할,
닉네임, 프로필 이미지가 달라질 수 있다.

### Decision

워크스페이스 권한은 `OWNER`, `ADMIN`, `MEMBER` 역할로 구분한다.
워크스페이스가 없는 상태에서도 사용할 계정 기본 프로필은 `users`에
저장한다.
워크스페이스 생성·참여 시 계정 기본 프로필을 멤버십 프로필로 복사하고,
이후 워크스페이스별 닉네임과 프로필 이미지는
`workspace_memberships`에서 독립적으로 관리한다.
메시지 작성, 초대 링크 발급, 파일 업로드 같은 행위 주체도
워크스페이스 멤버십 FK를 우선 사용한다.

### Reason

PRD는 워크스페이스 역할 구조와 워크스페이스별 프로필을 정의한다.
Architecture와 ERD는 워크스페이스 멤버십이 역할뿐 아니라
워크스페이스별 프로필의 기준이라고 명시한다.

### Consequences

- 권한 검증은 전역 사용자 ID만으로 처리하지 않고 워크스페이스
  멤버십을 기준으로 수행한다.
- 같은 사용자가 워크스페이스마다 다른 표시 이름과 프로필 이미지를
  가질 수 있다.
- 계정 기본 프로필 변경은 기존 워크스페이스 프로필에 전파하지 않는다.
- 행위 이력은 워크스페이스 문맥을 보존할 수 있다.

### Source

- `docs/prd.md`
- `docs/Architecture.md`
- `docs/ERD.md`
- `docs/API.md`

## Decision 007. 채널은 기본 채널, 공개 채널, 비공개 채널 정책으로 운영

### Status

Accepted

### Context

Re-Echo의 핵심 협업 단위는 워크스페이스 내부 채널이다.
워크스페이스 참여자는 기본 대화 공간에 진입해야 하고, 채널 공개 범위에
따라 접근 정책이 달라져야 한다.

### Decision

워크스페이스 생성 또는 참여 시 기본 채널 `#general`을 보장한다.
`#general`은 필수 참여 채널이며 나갈 수 없다.
채널은 `PUBLIC`과 `PRIVATE`로 구분하고, 공개 채널과 비공개 채널의
참여, 접근, 멤버 목록 공개 정책을 분리한다.

### Reason

PRD와 API 문서가 `#general`, 공개 채널, 비공개 채널의 정책을
구체적으로 확정한다.
ERD는 채널 가시성과 채널 멤버십을 별도 엔티티와 상태로 모델링한다.

### Consequences

- 워크스페이스에는 `#general` 채널이 하나만 존재해야 한다.
- 공개 채널은 워크스페이스 멤버라면 자유롭게 참여할 수 있다.
- 비공개 채널 접근은 채널 멤버십 기준으로 추가 검증해야 한다.
- 채널 참여는 사용자 계정이 아니라 워크스페이스 멤버십 기준으로
  연결한다.

### Source

- `docs/prd.md`
- `docs/ERD.md`
- `docs/API.md`

## Decision 008. 실시간 처리는 메시지와 입력 중 표시로 제한

### Status

Accepted

### Context

1차 MVP는 채널 기반 실시간 협업 경험을 제공해야 하지만, 모든 상태를
실시간 이벤트로 전파할 필요는 없다.

### Decision

WebSocket 실시간 처리는 메시지 송수신과 입력 중 표시로 제한한다.
이벤트 타입은 `MESSAGE_CREATED`, `MESSAGE_UPDATED`, `MESSAGE_DELETED`,
`TYPING_UPDATED`만 사용한다.
메시지 이벤트와 typing 이벤트는 부분 변경이 아니라 snapshot 형태로
전달한다.

### Reason

PRD와 API 문서가 실시간 이벤트 타입과 payload 정책을 확정한다.
Architecture는 실시간 처리 범위를 메시지 송수신과 입력 중 표시로
제한한다.

### Consequences

- 읽음 상태, 멤버 목록, 채널 목록 같은 다른 상태를 임의로 WebSocket
  이벤트에 추가하지 않는다.
- 클라이언트는 `eventId`로 중복 이벤트를 제거한다.
- 메시지 최신성 판단은 `occurredAt`이 아니라
  `payload.message.updatedAt`을 기준으로 한다.

### Source

- `docs/prd.md`
- `docs/Architecture.md`
- `docs/API.md`
- `docs/coding-convention.md`

## Decision 009. 메시지 목록은 cursor pagination으로 조회

### Status

Accepted

### Context

채팅 화면은 최신 메시지 기준으로 진입하고, 과거 메시지는 위로 스크롤하며
로드해야 한다.

### Decision

메시지 목록은 cursor pagination을 사용한다.
cursor 기준값은 `createdAt + messageId` 조합으로 두고, cursor는 opaque
string이 아니라 객체 형태로 노출한다.
응답 page 메타데이터는 더 과거 메시지를 조회하기 위한 `nextCursor`를
제공한다.

### Reason

PRD와 API 문서가 메시지 목록의 cursor pagination과 cursor 기준값을
명시한다.
Coding Convention은 메시지 목록 cursor 구조를 API 문서 기준 그대로
따르도록 한다.

### Consequences

- 메시지 조회 API와 구현 DTO는 `createdAt`, `messageId` 기준 cursor
  구조를 유지해야 한다.
- offset pagination을 메시지 목록의 기본 방식으로 바꾸지 않는다.
- 동일 시각 메시지의 안정적 정렬을 위해 `messageId`를 함께 사용한다.

### Source

- `docs/prd.md`
- `docs/API.md`
- `docs/coding-convention.md`

## Decision 010. 읽음 상태는 채널별 마지막 읽은 메시지 기준으로 관리

### Status

Accepted

### Context

1차 MVP는 채널 단위 안읽음 배지와 읽지 않은 메시지 개수를 제공해야
하지만, 메시지별 읽음 상세 표시는 후속 확장 후보에 가깝다.

### Decision

읽음 상태는 메시지별 영수증 테이블이 아니라 채널별 마지막 읽은 메시지
기준점으로 관리한다.
읽음 갱신은 REST API로만 처리하고 별도 read broadcast는 제공하지 않는다.

### Reason

PRD와 API 문서는 읽음 갱신을 REST API로만 처리하고 별도 broadcast를
제공하지 않는다고 명시한다.
ERD와 API 문서는 읽음 상태를 채널별 마지막 읽은 메시지 기준으로
단순화한다고 명시한다.

### Consequences

- 안읽음 계산은 `channel_read_states`의 마지막 읽은 메시지 기준으로
  수행한다.
- 메시지별 사용자 읽음 영수증 테이블은 MVP ERD에 포함하지 않는다.
- WebSocket 이벤트에 읽음 상태 전용 이벤트를 임의로 추가하지 않는다.

### Source

- `docs/prd.md`
- `docs/ERD.md`
- `docs/API.md`
- `docs/coding-convention.md`

## Decision 011. 파일은 R2 직접 전송과 DB 메타데이터로 분리

### Status

Accepted

### Context

Re-Echo는 파일 첨부와 이미지 미리보기를 제공해야 하며, 파일 바이너리를
애플리케이션 서버가 직접 중계하면 서버 부하와 운영 복잡도가 커질 수
있다.

### Decision

파일 바이너리는 Cloudflare R2에 저장하고, 파일 메타데이터는
PostgreSQL에서 관리한다.
업로드와 다운로드는 Presigned URL 기반 직접 전송으로 처리한다.
파일 첨부는 업로드와 메시지 연결을 분리한다.
프로필 이미지도 동일한 R2 직접 업로드 구조를 사용하되, 이미지 전용
Presigned URL과 파일 메타데이터를 프로필에 연결하는 방식으로 처리한다.

### Reason

PRD, Architecture, ERD, API 문서가 파일 저장소를 외부 오브젝트
스토리지로 두고 Presigned URL 방식으로 직접 업로드/다운로드한다고
명시한다.
API 문서는 이 방식이 대용량 바이너리가 애플리케이션 서버를 통과하지
않게 하기 위한 결정이라고 설명한다.

### Consequences

- 서버는 파일 바이너리를 직접 저장하지 않고 권한 검증, Presigned URL
  발급, 메타데이터 관리에 집중한다.
- 메시지 생성 또는 수정 시 `fileId`가 연결되어야 최종 첨부로 확정된다.
- 프로필 수정 시 `profileImageFileId`가 전달되면 서버가 파일 소유자,
  용도, 업로드 완료 여부를 검증한 뒤 프로필 이미지로 연결한다.
- 업로드만 완료되고 메시지나 프로필 이미지에 연결되지 않은 orphan
  파일은 정기 배치로 정리해야 한다.
- 프로필 이미지 정리 배치는 기본적으로 24시간 이상 참조되지 않은
  파일을 하루 1회 처리하며, 삭제 직전 참조 여부를 다시 확인해야 한다.

### Source

- `docs/prd.md`
- `docs/Architecture.md`
- `docs/ERD.md`
- `docs/API.md`
- `docs/coding-convention.md`

## Decision 012. 워크스페이스와 채널은 보관 후 만료 삭제 모델 사용

### Status

Accepted

### Context

워크스페이스와 채널 삭제는 사용자 협업 데이터와 메시지 접근성에 큰
영향을 주기 때문에 즉시 물리 삭제보다 복원 가능한 상태 전환이 필요하다.

### Decision

워크스페이스와 채널은 즉시 삭제하지 않고 `ARCHIVED` 상태로 전환한다.
보관 후 15일 이내에는 복원 가능하고, 15일 경과 후 자동 삭제한다.
보관된 워크스페이스와 채널은 삭제 전까지 읽기 전용으로 탐색 가능하다.

### Reason

PRD, Architecture, ERD, API 문서가 보관, 복원, 15일 후 자동 삭제,
읽기 전용 탐색 정책을 반복해서 명시한다.

### Consequences

- 삭제 요청은 즉시 물리 삭제가 아니라 상태 전환으로 구현한다.
- `archived_at`, `archive_expires_at`, `deleted_at` 같은 상태 추적
  컬럼이 필요하다.
- 보관 상태에서는 메시지 작성, 수정, 삭제 같은 변경 작업을 제한해야
  한다.
- 정리 배치가 만료된 워크스페이스, 채널, 관련 파일을 처리해야 한다.

### Source

- `docs/prd.md`
- `docs/Architecture.md`
- `docs/ERD.md`
- `docs/API.md`

## Decision 013. REST API는 공통 응답 envelope과 에러 코드 체계를 사용

### Status

Accepted

### Context

클라이언트와 서버는 인증, 워크스페이스, 채널, 메시지, 파일 등 다양한
리소스를 일관된 형식으로 주고받아야 한다.

### Decision

REST API base path는 `/api/v1`로 둔다.
모든 REST API 응답은 `status`, `errorCode`, `message`, `result`를
포함하는 공통 envelope을 사용한다.
목록 응답 필드명은 `items`가 아니라 `contents`를 사용한다.
공통 에러 코드는 prefix 없이 사용하고, 도메인 에러 코드는 도메인별
prefix를 사용한다.

### Reason

PRD와 API 문서가 공통 응답 envelope, 목록 필드명, 에러 코드 prefix
체계를 명시한다.
Coding Convention은 API 문서에 없는 응답 형식과 에러 코드를 임의로
추가하지 않도록 제한한다.

### Consequences

- Controller 응답 DTO와 예외 핸들러는 API 문서의 envelope 구조를
  유지해야 한다.
- Validation 실패 응답은 `fieldErrors`를 포함해야 한다.
- API 구현 편의를 이유로 응답 형식, 목록 필드명, 에러 코드 체계를
  바꾸지 않는다.

### Source

- `docs/prd.md`
- `docs/API.md`
- `docs/coding-convention.md`

## Decision 014. DTO와 Entity를 분리하고 계층 책임을 명확히 유지

### Status

Accepted

### Context

API 계약과 영속 모델이 직접 결합되면 엔티티 변경이 클라이언트 계약에
영향을 주고, Controller나 Repository에 비즈니스 정책이 섞일 수 있다.

### Decision

Controller 경계에서는 DTO만 사용하고 Entity를 API 응답으로 직접
노출하지 않는다.
Controller, Service, Repository, Entity, DTO의 책임을 분리한다.
Mapper는 DTO와 도메인/엔티티 변환만 담당한다.

### Reason

API 문서는 DB 엔티티 전체를 그대로 노출하지 않고 화면 동작에 필요한
DTO만 노출한다고 명시한다.
Coding Convention은 DTO와 Entity를 항상 분리하고 계층 책임을 섞지
않도록 규정한다.

### Consequences

- Response는 명시적 Response DTO로 구성한다.
- Entity 직렬화로 API 응답을 만들지 않는다.
- Repository는 영속성 접근만 담당하고 권한 판단이나 도메인 정책을
  구현하지 않는다.
- Mapper에 비즈니스 분기나 권한 판단을 넣지 않는다.

### Source

- `docs/API.md`
- `docs/coding-convention.md`

## Decision 015. Command Service와 Query Service를 책임 기준으로 분리

### Status

Accepted

### Context

워크스페이스, 채널, 메시지, 파일 도메인은 쓰기 유스케이스와 읽기
유스케이스의 트랜잭션 성격과 책임이 다르다.

### Decision

Service는 `CommandService`와 `QueryService` suffix를 명확히 사용한다.
쓰기 유스케이스는 Command Service에서 처리하고 트랜잭션 경계를 가진다.
읽기 유스케이스는 Query Service에서 처리하고 기본적으로
`readOnly` 트랜잭션을 사용한다.

### Reason

Coding Convention이 Command Service와 Query Service의 네이밍,
책임, 트랜잭션 규칙을 명시한다.
동시에 별도 읽기 DB, 이벤트 소싱, 메시지 브로커 기반 CQRS는 기본
전제가 아니라고 범위를 제한한다.

### Consequences

- 쓰기 로직과 조회 조합 로직이 같은 서비스에 과도하게 섞이지 않는다.
- Controller와 Repository에는 트랜잭션을 선언하지 않는다.
- CQRS는 코드 구조와 책임 분리 수준에서 적용하며 인프라 분리는
  MVP 기본 전제로 삼지 않는다.

### Source

- `docs/coding-convention.md`

## Decision 016. 상태값은 문자열 enum 컬럼으로 시작

### Status

Accepted

### Context

MVP에는 사용자, 멤버십, 초대 링크, 채널, 메시지, 파일 등에 여러 상태값이
필요하지만, 초기 단계부터 별도 코드 테이블을 도입하면 구조가 복잡해질 수
있다.

### Decision

상태값은 별도 코드 테이블보다 enum 수준의 문자열 컬럼으로 시작한다.
의미 없는 숫자 코드 저장은 피한다.

### Reason

ERD는 상태값을 각 엔티티의 문자열 상태 컬럼과 enum 정의로 정리하고,
상태값은 별도 코드 테이블보다 enum 수준의 문자열 컬럼으로 시작한다고
명시한다.
Coding Convention도 enum은 문자열 컬럼 저장을 기본으로 한다.

### Consequences

- 상태값은 `ACTIVE`, `ARCHIVED`, `DELETED`처럼 의미가 드러나는 문자열로
  저장한다.
- 코드 테이블 기반 관리가 필요한 수준의 공통 코드 체계는 MVP 기본
  구조에 포함하지 않는다.
- 상태값 변경 시 ERD, API 에러/응답 계약, 구현 enum을 함께 검토해야
  한다.

### Source

- `docs/ERD.md`
- `docs/coding-convention.md`

## Decision 017. Flyway 마이그레이션으로 DB 변경 이력을 관리

### Status

Accepted

### Context

PostgreSQL을 최종 진실 소스로 사용하므로 스키마 변경은 재현 가능하고
추적 가능한 방식으로 관리해야 한다.

### Decision

DB 변경은 Flyway 형식 마이그레이션으로 관리한다.
하나의 마이그레이션은 하나의 응집된 변경 단위를 가지며, 이미 배포된
마이그레이션 파일은 수정하지 않는다.

### Reason

Coding Convention이 Flyway 형식 마이그레이션 사용, 파일명 형식,
응집된 변경 단위, 배포된 마이그레이션 파일 수정 금지를 명시한다.

### Consequences

- 스키마 변경은 버전이 부여된 SQL 파일로 추적한다.
- 배포된 마이그레이션을 수정하는 대신 새 마이그레이션으로 변경을
  누적해야 한다.
- ERD 변경이 발생하면 마이그레이션 작성 단위와 배포 이력을 함께
  고려해야 한다.

### Source

- `docs/coding-convention.md`
