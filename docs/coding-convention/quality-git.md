# Re-Echo Coding Convention: 품질, Git, 변경 정책

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
- 테스트 객체는 다양한 상태 구성이 필요한 경우 builder를 사용할 수 있다.

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

### 15.3 Commit Separation

- 문서, 백엔드, 프런트 변경은 가능한 경우 서로 다른 커밋으로 분리한다.
- 하나의 기능이 여러 영역을 수정하더라도 각 영역이 독립적으로 검증 가능한
  단위라면 백엔드 API 구현, 프런트 연동, 문서 변경을 나눠 커밋한다.
- 단, 분리된 커밋이 빌드나 테스트를 깨뜨리거나 계약과 구현이 일시적으로
  크게 어긋나는 경우에는 하나의 커밋으로 묶을 수 있다.
- 커밋 전에는 staged 파일 목록을 확인해 의도하지 않은 영역의 변경이
  포함되지 않았는지 확인한다.
- 사용자가 구현, 수정, 반영을 요청한 작업은 검증이 끝난 뒤 커밋 분리
  원칙에 따라 커밋까지 수행한다.
- 원격 저장소 push는 사용자가 직접 수행하므로 자동으로 실행하지 않는다.

### 15.4 PR Rules

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
