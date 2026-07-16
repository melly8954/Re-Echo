# Re-Echo Decision Records: API와 구현 규칙

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
