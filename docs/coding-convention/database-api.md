# Re-Echo Coding Convention: 데이터베이스와 API 구현


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
- cursor 기반 목록 응답은 `result.contents`와 `result.page`를 가진
  공통 wrapper DTO를 사용한다.
- 메시지 목록 cursor 구조는 `docs/API.md`의 `createdAt + messageId`
  기준을 그대로 따른다.
- 메시지 목록의 `page.nextCursor`는 `createdAt`, `messageId`를 가진
  별도 cursor DTO로 표현한다.
- 메시지 목록에서 `nextCursor`는 더 과거 메시지 조회 기준으로 사용한다.
- 읽음 처리 API는 REST로만 구현하고 별도 read broadcast를 추가하지
  않는다.
- 파일 API는 서버 중계 업로드가 아니라 Presigned URL 기반으로 구현한다.
- 프로필 이미지 업로드도 서버 중계 없이 Presigned URL 기반으로
  구현하고, 프로필 수정 시 파일 소유자, 용도, 업로드 완료 여부를
  서버에서 검증한다.
- WebSocket 이벤트는 partial patch가 아니라 snapshot payload를 보낸다.

## 12. Exception Handling Convention

- 비즈니스 예외 클래스는 공통 `BusinessException`을 기본으로 사용한다.
- 각 도메인은 자신의 에러 코드 타입을 도메인 패키지 내부
  `exception` 패키지에 둔다.
  예: `workspace.exception.WorkspaceErrorCode`
- 도메인별로 별도 예외 클래스를 만들지는 않는다.
  단, 별도 catch 분기나 도메인 전용 속성이 필요해지면 그때 추가한다.
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
