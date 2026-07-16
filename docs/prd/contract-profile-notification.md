# Re-Echo 1차 MVP PRD: 공통 계약, 프로필, 알림

### 8.8 공통 API/실시간 계약

- REST API는 공통 응답 envelope를 사용
  - `status`는 int 형식 사용
  - 기본 형태는 `status`, `errorCode`, `message`, `result`를 포함
- validation 실패 응답은 `fieldErrors`를 포함
- 목록 응답 필드명은 `items`가 아니라 `contents`를 사용
- pagination은 요청/응답 모두 공통 구조를 사용
- pagination 방식별 세부 값은 `page.type` 하위 구조에 둠
- cursor는 opaque string 대신 객체 형태로 노출
- 전역 공통 에러 코드는 prefix 없이 사용
  - `INTERNAL_SERVER_ERROR`
  - `INVALID_REQUEST`
  - `VALIDATION_ERROR`
- 도메인 에러 코드는 아래 prefix 체계를 사용
  - `AUTH_*`
  - `WORKSPACE_*`
  - `CHANNEL_*`
  - `MESSAGE_*`
  - `FILE_*`
  - `INVITE_*`
  - `MEMBER_*`
- `COMMON_*`, `VALIDATION_*` 같은 별도 공통 prefix는 두지 않음
- 초대 링크 관련 에러는 저장 테이블명과 무관하게 `INVITE_*`를 사용
- WebSocket은 공통 event envelope를 사용
- 실시간 이벤트 타입은 아래 네 가지만 사용
  - `MESSAGE_CREATED`
  - `MESSAGE_UPDATED`
  - `MESSAGE_DELETED`
  - `TYPING_UPDATED`
- message 이벤트 payload는 부분 변경이 아니라 full snapshot을 전달
- typing 이벤트 payload는 `typingUserIds` 같은 snapshot을 전달
- 이벤트 중복 제거용 `eventId`를 포함
- 클라이언트 중복 제거 기준은 `eventId`
- 메시지 최신성 판단 기준은 `occurredAt`이 아니라
  `payload.message.updatedAt`

### 8.9 프로필

- 계정 기본 표시 이름과 프로필 이미지 지원
- 워크스페이스별 닉네임 지원
- 워크스페이스별 프로필 이미지 지원
- 워크스페이스별 닉네임 중복 허용
- 프로필 이미지는 R2 직접 업로드 후 파일 메타데이터를 프로필에
  연결한다
- 최초 OAuth Provider에서 받은 외부 프로필 이미지는 사용자가 직접
  업로드한 R2 이미지로 교체하기 전까지 계정 기본값으로 사용할 수 있다
- 워크스페이스 생성·참여 시 계정 기본 프로필을 멤버십 프로필로 복사
- 계정 기본 프로필과 기존 멤버십 프로필은 복사 이후 독립적으로 수정

### 8.10 안읽음/알림

- 채널 단위 안읽음 배지 지원
- 채널별 읽지 않은 메시지 개수 표시
- 채널 목록 배지만 표시
- 채널 진입 시 해당 시점까지 자동 읽음 처리
- 인앱 푸시 없음
- 브라우저 알림 없음
