# Re-Echo Coding Convention

## 1. Coding Convention Overview

이 문서는 Re-Echo MVP 구현 시 코드 구조, 계층 책임, 네이밍,
예외 처리, 테스트, Git 규칙을 일관되게 유지하기 위한 기준이다.

이 문서는 기술 선택을 새로 결정하는 문서가 아니다.
이미 확정된 PRD, Architecture, ERD, API 문서를 구현 코드로
안전하게 옮기기 위한 규칙만 정의한다.

## 2. Source Documents

- `docs/prd.md`
- `docs/Architecture.md`
- `docs/ERD.md`
- `docs/API.md`

참고:

- `docs/API.md`는 클라이언트와 서버의 계약 문서다.
- Endpoint, Request/Response DTO, 공통 응답 형식, 에러 코드,
  인증 방식, 실시간 이벤트 형식은 `docs/API.md`를 우선 따른다.
- 이 문서는 API 구현 방식과 코드 배치 원칙만 보완한다.

## 3. Document Priority

문서 간 충돌 시 우선순위는 다음과 같다.

1. `docs/prd.md`
2. `docs/Architecture.md`
3. `docs/ERD.md`
4. `docs/API.md`
5. `docs/coding-convention.md`

원칙:

- 제품 정책과 범위는 PRD를 따른다.
- 시스템 구조와 기술 선택은 Architecture를 따른다.
- 데이터 구조와 상태 값은 ERD를 따른다.
- API 계약은 API 문서를 따른다.
- Coding Convention은 상위 문서를 구현 코드로 옮기는 규칙만
  정의한다.

## 4. Common Principles

- 최소한의 추상화로 시작한다.
- 구조가 명확해지기 전까지 과도한 공통화는 하지 않는다.
- Architecture에 없는 패턴을 임의로 도입하지 않는다.
- Controller, Service, Repository, Entity, DTO의 책임을 섞지
  않는다.
- DTO와 Entity는 항상 분리한다.
- 서버 상태와 UI 상태는 분리한다.
- 읽기와 쓰기 유스케이스는 CQRS 관점에서 분리한다.
- 구현 편의보다 유지보수성과 계약 일관성을 우선한다.
- API 응답 형식은 구현 편의로 바꾸지 않고 `docs/API.md`에 맞춘다.

## 5. Naming Convention

### 5.1 Common Rules

- 이름은 역할이 드러나야 한다.
- 축약어는 팀에서 자주 쓰는 경우만 허용한다.
- 모호한 짧은 이름보다 긴 명확한 이름을 우선한다.

### 5.2 Backend Naming

- Controller: `WorkspaceController`
- Command Service: `CreateWorkspaceCommandService`
- Query Service: `WorkspaceQueryService`
- Repository: `WorkspaceRepository`
- Entity: `Workspace`, `Channel`, `Message`
- Mapper: `WorkspaceMapper`
- Request DTO: `CreateWorkspaceRequest`
- Response DTO: `WorkspaceResponse`, `ChannelMessageResponse`
- Exception: `WorkspaceArchivedException`, `InviteLinkExpiredException`

원칙:

- Service는 `CommandService`, `QueryService` suffix를 명확히 쓴다.
- DTO는 `Request`, `Response`로 구분한다.
- Java DTO는 특별한 이유가 없으면 `record`로 작성한다.
- 범용 `Dto`, `Vo`, `Util` 같은 이름은 피한다.
- API 문서의 리소스 이름과 코드의 DTO 이름이 크게 어긋나지 않게
  유지한다.

### 5.3 Frontend Naming

- React 컴포넌트: `PascalCase`
- hooks: `useCamelCase`
- store: `useXxxStore` 또는 `xxxStore`
- CSS Module: 컴포넌트와 같은 이름 + `.module.css`

예:

- `MessageInput.tsx`
- `MessageInput.module.css`

### 5.4 Database Naming

- 테이블명: `snake_case` 복수형
- 컬럼명: `snake_case`
- FK 컬럼명은 참조 대상을 포함한다.

예:

- `workspace_memberships`
- `channel_read_states`
- `workspace_id`
- `created_by_user_id`

## 6. Project Structure Convention

### 6.1 Backend Structure

백엔드는 `도메인 중심 + 공통 계층 분리` 구조를 따른다.

예시:

```text
backend
├─ common
├─ config
├─ security
├─ infra
├─ auth
├─ workspace
├─ channel
├─ message
└─ file
```

도메인 패키지 예시:

```text
workspace
├─ controller
├─ service
│  ├─ command
│  └─ query
├─ dto
├─ domain
├─ repository
├─ mapper
└─ exception
```

원칙:

- 도메인 패키지 안에 해당 도메인의 Controller, Service, DTO,
  Repository를 함께 둔다.
- `config`, `security`, `common`, `infra`는 별도 상위 패키지로 둔다.
- 여러 도메인에서 실제로 공유되는 코드만 `common`으로 올린다.

### 6.2 Frontend Structure

프론트는 `pages / features / components / shared / stores` 구조를 따른다.

예시:

```text
src
├─ pages
├─ features
│  ├─ auth
│  ├─ workspace
│  ├─ channel
│  ├─ message
│  └─ file
├─ components
├─ shared
├─ hooks
└─ stores
```

원칙:

- `pages`는 라우트 진입 화면과 화면 조합만 담당한다.
- `features`는 도메인별 UI, hooks, query wrapper를 가진다.
- `components`는 도메인 비의존 공통 UI만 둔다.
- `shared`는 공통 API client, websocket client, constants, utils를 둔다.
- `stores`는 인증 보조 상태, 모달, 토스트 같은 전역 UI 상태만 둔다.

## 7. Layer Responsibility

### 7.1 Controller

- Request DTO를 입력받는다.
- 인증 사용자와 요청 파라미터를 유스케이스 입력으로 변환한다.
- 비즈니스 로직을 직접 구현하지 않는다.
- Entity를 직접 반환하지 않는다.
- 응답 형식은 `docs/API.md`의 계약을 기준으로 맞춘다.

### 7.2 Command Service

- 쓰기 유스케이스를 처리한다.
- 트랜잭션 경계를 가진다.
- 여러 Repository 호출을 조합한다.
- 도메인 규칙 위반 시 비즈니스 예외를 던진다.

### 7.3 Query Service

- 읽기 유스케이스를 처리한다.
- 기본적으로 `readOnly` 트랜잭션을 사용한다.
- 조회 최적화와 응답 조합을 담당한다.
- 상태 변경 로직을 두지 않는다.

### 7.4 Domain

- 핵심 도메인 상태와 규칙을 표현한다.
- 프레임워크 의존 코드는 최소화한다.
- 외부 시스템 접근을 직접 하지 않는다.

### 7.5 Repository

- 영속성 접근만 담당한다.
- 비즈니스 정책을 구현하지 않는다.
- 조회 목적의 커스텀 쿼리는 Query Service 요구사항 기준으로만
  추가한다.

### 7.6 Mapper

- DTO와 도메인/엔티티 변환만 담당한다.
- 비즈니스 분기나 권한 판단을 넣지 않는다.

## 8. Backend Convention

### 8.1 DTO / Entity Separation

- Controller 경계에서는 DTO만 사용한다.
- Entity를 API 응답으로 직접 노출하지 않는다.
- Request DTO와 Response DTO는 불변 값 전달 객체로 다루며,
  Java에서는 기본적으로 `record`를 사용한다.
- DTO에 비즈니스 로직, 권한 판단, 영속성 의존 코드를 넣지 않는다.
- Request DTO 검증은 Controller 입력 경계에서 수행한다.
- Response DTO 구조는 `docs/API.md`의 응답 예시를 기준으로 맞춘다.

### 8.2 Transaction Rules

- 쓰기 유스케이스는 Command Service에서 `@Transactional`을 선언한다.
- 읽기 유스케이스는 Query Service에서
  `@Transactional(readOnly = true)`를 사용한다.
- Controller와 Repository에 트랜잭션을 선언하지 않는다.

### 8.3 CQRS Scope

- Command와 Query는 코드 구조와 책임 분리 수준에서 적용한다.
- 별도 읽기 DB, 이벤트 소싱, 메시지 브로커 기반 CQRS는 기본 전제가
  아니다.

### 8.4 Exception Handling

- 도메인별 비즈니스 예외 클래스를 둔다.
- 도메인별 예외와 에러 코드는 각 도메인 패키지의 `exception`
  패키지가 소유한다.
- 모든 도메인 에러 코드를 하나의 전역 `ErrorCode` enum에
  집중시키지 않는다.
- 전역 예외 핸들러가 비즈니스 예외를 공통 응답 형식으로 변환한다.
- Validation, 인증, 인가 예외도 전역 핸들러에서 처리한다.
- 예외 메시지와 로그 메시지는 구분한다.
- 에러 코드는 `docs/API.md`에 정의된 prefix와 이름을 그대로 사용한다.

### 8.5 Security

- 토큰, 쿠키, OAuth 민감정보를 로그에 남기지 않는다.
- 인증 사용자 정보는 Security Context 기반으로 주입한다.
- 최종 권한 검증은 Service 유스케이스 안에서 수행한다.
- WebSocket handshake 인증도 HTTP 인증과 같은 Access Token 기준을
  따른다.

## 9. Frontend Convention

### 9.1 State Management

- 서버 상태는 React Query로 관리한다.
- 전역 UI 상태는 store로 관리한다.
- 서버 응답 원본을 store에 중복 저장하지 않는다.

### 9.2 API / WebSocket Access

- 공통 API client와 WebSocket client를 `shared` 계층에 둔다.
- 기능별 query/mutation hook은 `features` 아래에 둔다.
- 인증, base URL, 공통 에러 처리, 재연결 같은 횡단 관심사는 공통
  클라이언트에서 처리한다.
- WebSocket event type과 payload 구조는 `docs/API.md`를 그대로 따른다.

### 9.3 TypeScript Rules

- `props`, `API request/response`, `query result`, `store state` 타입을
  명시한다.
- 타입이 과도하게 복잡해지면 단순한 인터페이스나 타입 별칭을 우선한다.
- 불필요한 범용 제네릭 유틸리티 타입은 피한다.
- API 타입은 서버 계약과 같은 이름 체계를 유지한다.

### 9.4 Styling

- 기본 스타일은 `CSS Module` 중심으로 간다.
- 전역 스타일은 reset, theme token, layout shell 정도로 최소화한다.
- 컴포넌트 스타일 파일은 같은 위치에 colocate한다.
- 인라인 스타일은 동적 값이 꼭 필요한 경우에만 사용한다.

## 10. Database Convention

### 10.1 Database Rules

- 테이블명과 컬럼명은 `snake_case`를 사용한다.
- 조인 테이블과 상태 테이블도 의미가 드러나는 이름을 사용한다.

### 10.2 Common Columns

- 시간 컬럼은 `created_at`, `updated_at`를 기본으로 사용한다.
- 보관/삭제가 필요한 경우 `archived_at`, `archive_expires_at`,
  `deleted_at` 같은 명시적 컬럼명을 사용한다.

### 10.3 Enum Storage

- Enum은 문자열 컬럼으로 저장하는 것을 기본으로 한다.
- 의미 없는 숫자 코드 저장은 피한다.

### 10.4 Migration

- Flyway 형식 마이그레이션을 사용한다.
- 파일명 형식 예시
  - `V1__init.sql`
  - `V2__create_workspace_tables.sql`
- 하나의 마이그레이션은 하나의 응집된 변경 단위를 가진다.
- 이미 배포된 마이그레이션 파일은 수정하지 않는다.

## 11. API Implementation Convention

- Controller는 Request DTO를 검증한 뒤 Service로 전달한다.
- Response는 항상 명시적 Response DTO를 사용한다.
- Java Request/Response DTO는 특별한 상태 변경이 필요하지 않으므로
  기본적으로 `record`로 작성한다.
- Entity 직렬화로 응답을 구성하지 않는다.
- 공통 응답 envelope은 `status`, `errorCode`, `message`, `result`
  구조를 유지한다.
- 목록 응답 필드명은 `items`가 아니라 `contents`를 사용한다.
- 메시지 목록 cursor 구조는 `docs/API.md`의 `createdAt + messageId`
  기준을 그대로 따른다.
- 읽음 처리 API는 REST로만 구현하고 별도 read broadcast를 추가하지
  않는다.
- 파일 API는 서버 중계 업로드가 아니라 Presigned URL 기반으로 구현한다.
- WebSocket 이벤트는 partial patch가 아니라 snapshot payload를 보낸다.

## 12. Exception Handling Convention

- 비즈니스 예외는 도메인 패키지 내부 `exception` 패키지에 둔다.
- 각 도메인은 자신의 예외 클래스와 에러 코드 타입을 가진다.
  예: `workspace.exception.WorkspaceException`,
  `workspace.exception.WorkspaceErrorCode`
- 공통 예외 코드는 공통 영역에 두되, 도메인 예외 코드를 공통
  `ErrorCode` 하나로 합치지 않는다.
- 전역 예외 핸들러는 도메인 예외를 공통 응답 envelope으로 변환하는
  역할만 담당한다.
- 공통 예외 응답 형식은 전역 핸들러에서 정의한다.
- 예상 가능한 실패는 비즈니스 예외로 표현한다.
- 예상하지 못한 예외는 시스템 예외로 분리하고 상세 원인은 로그에만
  남긴다.
- 사용자 응답에는 내부 구현 정보와 스택 트레이스를 노출하지 않는다.
- Validation 에러는 `fieldErrors`를 포함한 응답으로 맞춘다.

## 13. Logging Convention

- 운영 로그 중심으로 작성한다.
- 요청 추적 ID를 사용할 수 있으면 함께 사용한다.
- 주요 비즈니스 이벤트, 외부 연동 실패, 예외 발생은 로그로 남긴다.
- Access Token, Refresh Token, Cookie, Authorization Header,
  개인정보 원문은 로그에 남기지 않는다.
- 개발 환경에서만 제한적으로 디버그 로그를 허용한다.
- 임시 디버그 로그는 작업 완료 후 제거한다.

## 14. Test Convention

- 도메인 비즈니스 규칙과 분기 로직은 단위 테스트로 검증한다.
- 인증, DB, 트랜잭션, API 흐름은 통합 테스트로 검증한다.
- Command Service와 Query Service의 테스트 목적을 구분한다.
- 권한 실패, 보관 상태, 초대 만료, 메시지 수정/삭제 권한 같은
  계약성 예외를 반드시 포함한다.
- API 테스트는 `docs/API.md`의 응답 형식과 에러 코드를 함께 검증한다.
- 테스트 이름은 의도가 드러나게 작성한다.

## 15. Git Convention

### 15.1 Branch Strategy

- 기능 개발: `feature/<short-name>`
- 버그 수정: `fix/<short-name>`
- 문서 작업: `docs/<short-name>`
- 리팩터링: `refactor/<short-name>`

예시:

- `feature/workspace-invite`
- `fix/channel-read-state`
- `docs/coding-convention`

### 15.2 Commit Message

형식:

```text
type: 변경 내용
```

타입:

- `feat`
- `fix`
- `refactor`
- `docs`
- `style`
- `test`
- `perf`
- `chore`
- `ci`
- `build`

원칙:

- 커밋 메시지는 한국어로 작성한다.
- 하나의 커밋은 하나의 목적을 갖도록 유지한다.

### 15.3 PR Rules

- PR은 가능한 작고 명확하게 유지한다.
- 구조 변경과 기능 변경이 함께 크면 분리한다.
- PR 설명에는 변경 이유, 영향 범위, 테스트 여부를 포함한다.

## 16. Forbidden Practices

- Entity를 API 응답으로 직접 반환하는 것
- Controller에서 비즈니스 로직을 처리하는 것
- Repository에서 권한 판단이나 도메인 정책을 구현하는 것
- 서버 응답 데이터를 전역 store에 중복 복사하는 것
- 공통화 근거 없이 범용 util과 추상 클래스를 늘리는 것
- 토큰, 쿠키, 개인정보를 로그에 남기는 것
- 배포된 Flyway 마이그레이션 파일을 수정하는 것
- API 문서에 없는 응답 형식과 에러 코드를 임의로 추가하는 것
- 모든 도메인의 에러 코드를 하나의 전역 `ErrorCode` enum에
  집중시키는 것
- 메시지 read broadcast 같은 문서 밖 실시간 이벤트를 임의로 추가하는 것

## 17. Change Policy

- 이 문서는 PRD, Architecture, ERD, API 변경에 종속된다.
- 상위 문서가 바뀌면 Coding Convention도 함께 검토한다.
- API 계약이 변경되면 DTO, 예외 코드, Controller 응답 규칙도 같이
  업데이트한다.
- 새 규칙을 추가할 때는 기존 규칙보다 더 단순하고 명확한지 먼저
  검토한다.
