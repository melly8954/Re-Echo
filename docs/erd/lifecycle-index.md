# Re-Echo 1차 MVP ERD: 수명주기와 인덱스


### 10.1 워크스페이스

- 삭제 방식
  - 즉시 물리 삭제가 아니라 `ARCHIVED` 상태로 전환
- 복원 정책
  - 보관 후 15일 이내 복원 가능
- 만료 정책
  - `archive_expires_at` 도달 시 최종 삭제 처리

### 10.2 채널

- 삭제 방식
  - 즉시 물리 삭제가 아니라 `ARCHIVED` 상태로 전환
- 복원 정책
  - 보관 후 15일 이내 복원 가능
- 만료 정책
  - `archive_expires_at` 도달 시 최종 삭제 처리

### 10.3 멤버십

- 자진 탈퇴는 `LEFT`
- 관리자 강제 제거는 `REMOVED`
- 자진 탈퇴 후 재참여는 기존 멤버십 행을 `ACTIVE`로 복구한다.
- 강제 제거된 멤버는 재가입할 수 없다.
- 비공개 채널 생성자는 채널 보관 전까지 자진 탈퇴와 강제 제거를
  허용하지 않는다.

### 10.4 메시지

- 메시지는 소프트 삭제를 기본으로 한다
- 삭제자는 `deleted_by_membership_id`로 추적한다
- 수정 이력 테이블은 MVP 범위에 포함하지 않는다

### 10.5 파일

- 파일 메타데이터 등록 실패 시 `ORPHANED` 상태가 될 수 있다
- 프로필 이미지가 제거되거나 교체되면 기존 파일은 `ORPHANED`로
  표시하고 `orphaned_at`을 기록한다
- 정기 스케줄러가 고아 파일과 만료 대상 파일을 정리한다
- 프로필 이미지 정리 대상은 `purpose = 'PROFILE_IMAGE'`이고
  `status IN ('ACTIVE', 'ORPHANED')`인 파일 중 사용자 계정과
  워크스페이스 멤버십에서 더 이상 참조하지 않는 파일이다
- `ORPHANED` 파일은 `orphaned_at`, 임시 `ACTIVE` 파일은 `created_at`이
  정리 기준 시각보다 이전일 때 정리 후보가 된다

## 11. Index Strategy

### 11.1 필수 유니크 인덱스

- `user_identities(provider, provider_user_id)`
- `workspace_memberships(workspace_id, user_id)`
- `workspace_invite_links(token)`
- `channels(workspace_id, name)`
- `channel_memberships(channel_id, workspace_membership_id)`
- `channel_read_states(channel_membership_id)`
- `message_attachments(message_id, file_object_id)`
- `file_objects(storage_key)`

### 11.2 주요 조회 인덱스

- `workspace_memberships(user_id, status, joined_at asc, id asc)`
  - 사용자가 속한 워크스페이스 목록의 등록 순 조회
- `workspace_memberships(workspace_id, display_name)`
  - 워크스페이스 멤버 목록 및 닉네임 조회
- `channels(workspace_id, status, visibility)`
  - 워크스페이스별 채널 목록 조회
- `channel_memberships(workspace_membership_id, status)`
  - 사용자의 채널 참여 목록 조회
- `messages(channel_id, created_at desc)`
  - 채널 메시지 최신 조회 및 과거 스크롤 조회
- `file_objects(workspace_id, status, uploaded_at desc)`
  - 워크스페이스 파일 정리 및 조회

### 11.3 부분 유니크 인덱스 제안

- 워크스페이스당 활성 초대 링크 1개 보장
  - 예: `workspace_id` where `status = 'ACTIVE'`
- 워크스페이스당 `#general` 채널 1개 보장
  - 예: `workspace_id` where `is_general = true`

## 12. Design Decisions

- 사용자 인증 공급자 정보는 `users`와 분리해 `user_identities`로 관리한다.
- 채널 참여는 사용자 계정이 아니라 워크스페이스 멤버십을 기준으로 연결한다.
- 메시지 작성, 파일 업로드, 초대 발급은 같은 행위 주체를 워크스페이스 문맥으로 보존하기 위해 멤버십 FK를 우선 사용한다.
- 계정 기본 프로필은 `users`에 저장하고 새 멤버십 프로필의 초기값으로
  사용한다.
- 워크스페이스별 닉네임과 프로필 이미지는 복사 이후
  `workspace_memberships`에서 독립적으로 관리한다.
- 읽음 상태는 메시지별 영수증 테이블이 아니라 채널별 마지막 읽은 메시지 기준으로 단순화한다.
- 파일은 R2에 저장하고 DB에는 메타데이터만 유지한다.
- 워크스페이스와 채널은 모두 보관 후 만료 삭제 모델을 사용한다.
- 상태값은 별도 코드 테이블보다 enum 수준의 문자열 컬럼으로 시작한다.
