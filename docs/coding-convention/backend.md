# Re-Echo Coding Convention: 백엔드 계층과 구현 규칙


### 7.1 Controller

- Request DTO를 입력받는다.
- 인증 사용자와 요청 파라미터를 유스케이스 입력으로 변환한다.
- 비즈니스 로직을 직접 구현하지 않는다.
- Entity를 직접 반환하지 않는다.
- 응답 형식은 `docs/API.md`의 계약을 기준으로 맞춘다.
- 공개 Endpoint Method에는 어떤 API 요청을 어떤 유스케이스로
  위임하는지 한국어 한 줄 주석으로 설명한다.

### 7.2 Command Service

- 쓰기 유스케이스를 처리한다.
- 트랜잭션 경계를 가진다.
- 여러 Repository 호출을 조합한다.
- 도메인 규칙 위반 시 비즈니스 예외를 던진다.
- 공개 Use Case Method에는 처리하는 비즈니스 행위와 반환 의미를
  한국어 한 줄 주석으로 설명한다.

### 7.3 Query Service

- 읽기 유스케이스를 처리한다.
- 기본적으로 `readOnly` 트랜잭션을 사용한다.
- 조회 최적화와 응답 조합을 담당한다.
- 상태 변경 로직을 두지 않는다.
- 공개 Use Case Method에는 조회 기준과 응답 조합 의도를 한국어 한 줄
  주석으로 설명한다.

### 7.4 Domain

- 핵심 도메인 상태와 규칙을 표현한다.
- 프레임워크 의존 코드는 최소화한다.
- 외부 시스템 접근을 직접 하지 않는다.

### 7.5 Repository

- 영속성 접근만 담당한다.
- 비즈니스 정책을 구현하지 않는다.
- 조회 목적의 커스텀 쿼리는 Query Service 요구사항 기준으로만
  추가한다.
- Spring Data 규칙만으로 의미가 명확한 기본 메서드는 주석을 생략할 수
  있다.
- 복합 조건, 권한 범위, 커서 기준처럼 의도가 필요한 커스텀 조회
  메서드는 한국어 한 줄 주석으로 설명한다.

### 7.6 Mapper

- DTO와 도메인/엔티티 변환만 담당한다.
- 비즈니스 분기나 권한 판단을 넣지 않는다.
- 공개 변환 Method에는 변환 방향과 API 계약상 필요한 이유를 한국어
  한 줄 주석으로 설명한다.

### 7.7 Comment

- 클래스, 인터페이스, enum, record, 설정 객체처럼 책임을 가진 주요 타입에는
  역할과 경계가 드러나는 한국어 한 줄 주석을 선언부 바로 위에 작성한다.
- 공개 Method에는 호출자가 알아야 할 유스케이스, 처리 기준 또는 반환 의미를
  한국어 한 줄 주석으로 작성한다. 기존 7.1~7.6의 계층별 규칙은 이 기준을
  구체화한 것으로 함께 적용한다.
- `private` Method와 내부 분기에는 이름만으로 의도를 알기 어려운 권한 검증,
  상태 전환, 외부 연동, 캐시·토큰 처리, 정렬·페이징 기준에 한해 왜 필요한지
  한국어 한 줄 주석으로 작성한다.
- 단순 getter, 생성자, 표준 CRUD, 단순 DTO 변환처럼 코드와 이름만으로 의미가
  분명한 구현에는 주석을 반복하지 않는다.
- 주석은 구현 절차를 줄줄이 번역하지 않고, 정책·제약·선택 이유를 설명한다.

## 8. Backend Convention

### 8.1 DTO / Entity Separation

- Controller 경계에서는 DTO만 사용한다.
- Entity를 API 응답으로 직접 노출하지 않는다.
- Request DTO와 Response DTO는 불변 값 전달 객체로 다루며,
  Java에서는 기본적으로 `record`를 사용한다.
- 단순 DTO는 record 생성자를 사용하고, 필드가 많아 생성자 가독성이
  떨어지는 Response DTO에만 builder 사용을 검토한다.
- Entity 변환처럼 생성 의미가 필요한 DTO는 `from(...)`, `of(...)`
  정적 팩터리 메서드를 사용할 수 있다.
- DTO에 비즈니스 로직, 권한 판단, 영속성 의존 코드를 넣지 않는다.
- Request DTO 검증은 Controller 입력 경계에서 수행한다.
- Response DTO 구조는 `docs/API.md`의 응답 예시를 기준으로 맞춘다.

### 8.2 Entity Mapping

- DB 스키마의 최종 기준은 Flyway 마이그레이션이다.
- JPA Entity는 DDL 생성용이 아니라 기존 테이블과 객체를 연결하는 매핑 코드로 작성한다.
- Entity 클래스에는 기본적으로 `@Entity`, `@Table(name = "...")`를 명시한다.
- Entity는 Lombok의 `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor(access = AccessLevel.PRIVATE)`, `@Builder(access = AccessLevel.PRIVATE)`를 기본으로 사용한다.
- Entity 생성은 공개 builder 대신 정적 팩터리 메서드로 노출한다.
- Entity에 공개 setter를 두지 않고, 상태 변경은 의도가 드러나는 도메인
  메서드로 처리한다.
- `@Column`과 `@JoinColumn`에는 기본적으로 `name`만 명시한다.
- `nullable`, `length`, `uniqueConstraints`처럼 Flyway DDL과 중복되는 제약은 Entity에 반복하지 않는다.
- 단, `updatable = false`, `insertable = false`처럼 JPA 동작 제어가 필요한 매핑 옵션은 필요한 경우 명시할 수 있다.
- `created_at`, `updated_at` 같은 시간 컬럼은 공통 BaseEntity 없이 각 Entity에 직접 선언한다.
- Entity 시간 타입은 구현 편의성을 우선해 `LocalDateTime`을 사용한다.
- `created_at`처럼 생성 후 변경되지 않아야 하는 필드는 `updatable = false`를 사용할 수 있다. 이는 DB 제약이 아니라 JPA가 update SQL에서 해당 컬럼을 제외하도록 하는 매핑 설정이다.
- Java 필드 초기값은 새 Entity 객체 생성 시 기본값이다. DB default는 INSERT에서 해당 컬럼이 생략될 때 적용되므로, 두 기본값이 충돌하지 않도록 같은 의미로 유지한다.
- `@Builder`를 사용하는 Entity 필드에 Java 기본값을 둘 경우 `@Builder.Default`를 함께 사용한다.
- UUID PK는 `@GeneratedValue(strategy = GenerationType.UUID)`를 사용한다.


### 8.3 Transaction Rules

- 쓰기 유스케이스는 Command Service에서 `@Transactional`을 선언한다.
- 읽기 유스케이스는 Query Service에서
  `@Transactional(readOnly = true)`를 사용한다.
- Controller와 Repository에 트랜잭션을 선언하지 않는다.

### 8.4 CQRS Scope

- Command와 Query는 코드 구조와 책임 분리 수준에서 적용한다.
- 별도 읽기 DB, 이벤트 소싱, 메시지 브로커 기반 CQRS는 기본 전제가
  아니다.

### 8.5 Exception Handling

- 예상 가능한 도메인 실패는 공통 `BusinessException`으로 표현한다.
- 도메인별 예외와 에러 코드는 각 도메인 패키지의 `exception`
  패키지가 소유한다.
- 모든 도메인 에러 코드를 하나의 전역 `ErrorCode` enum에
  집중시키지 않는다.
- 전역 예외 핸들러가 비즈니스 예외를 공통 응답 형식으로 변환한다.
- Validation, 인증, 인가 예외도 전역 핸들러에서 처리한다.
- 예외 메시지와 로그 메시지는 구분한다.
- 사용자 응답으로 노출될 수 있는 예외 메시지는 한국어로 작성한다.
- 도메인 객체나 서비스 내부의 방어적 인자 검증 메시지도 한국어로
  작성한다.
- 에러 코드는 `docs/API.md`에 정의된 prefix와 이름을 그대로 사용한다.

### 8.6 Security

- 토큰, 쿠키, OAuth 민감정보를 로그에 남기지 않는다.
- 민감정보가 포함된 record DTO는 `toString()` 결과를 로그에 남기지
  않는다.
- 인증 사용자 정보는 Security Context 기반으로 주입한다.
- 최종 권한 검증은 Service 유스케이스 안에서 수행한다.
- WebSocket handshake 인증도 HTTP 인증과 같은 Access Token 기준을
  따른다.

## 9. Frontend Convention
