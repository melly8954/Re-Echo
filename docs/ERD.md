# Re-Echo 1차 MVP ERD

## 1. ERD Overview

Re-Echo 1차 MVP ERD는 다음 흐름을 안정적으로 지원하는 것을 목표로 한다.

- 소셜 로그인
- 계정 기본 프로필
- 워크스페이스 생성 및 참여
- 역할 기반 멤버 관리
- 워크스페이스별 프로필 관리
- 공개 채널 및 비공개 채널 운영
- 실시간 메시지 송수신
- 파일 첨부
- 채널 단위 읽음 상태 추적
- 보관, 복원, 자동 삭제

이 ERD는 `docs/prd.md`와 `docs/Architecture.md`를 기준으로 작성한다.
MVP 범위 밖인 DM, 메시지 검색, 멘션, AI 기능은 포함하지 않는다.

## 2. Confirmed Data Requirements

### 2.1 확정된 기능 범위

- 사용자 계정은 Google, Kakao, GitHub 소셜 로그인으로 생성된다.
- 사용자는 여러 워크스페이스를 생성하거나 참여할 수 있다.
- 워크스페이스마다 `OWNER`, `ADMIN`, `MEMBER` 역할이 있다.
- 워크스페이스별 닉네임과 프로필 이미지를 관리한다.
- 워크스페이스 참여 시 기본 채널 `#general`에 자동 참여한다.
- 채널은 공개 채널과 비공개 채널로 구분된다.
- 메시지는 실시간으로 송수신되며 본인 메시지는 수정할 수 있다.
- 관리자는 모든 멤버 메시지를 삭제할 수 있다.
- 파일 첨부 메타데이터는 애플리케이션 DB에서 관리한다.
- 읽음 상태는 마지막 읽은 메시지 기준으로 채널 단위 계산을 사용한다.
- 워크스페이스와 채널은 보관 후 15일 뒤 자동 삭제된다.

### 2.2 확정된 제외 범위

- DM
- 메시지 검색
- 멘션
- 푸시 알림
- AI, 음성, 화상 관련 기능

### 2.3 확정된 데이터 운영 정책

- 최종 진실 소스는 PostgreSQL이다.
- Redis는 실시간 상태와 보조 운영 데이터 저장소이며 ERD 범위에 포함하지 않는다.
- 파일 바이너리는 Cloudflare R2에 저장하고 DB에는 메타데이터만 저장한다.
- 강제 신고, 멤버 신고 같은 제재 도메인은 없다.
- 워크스페이스와 채널은 즉시 삭제하지 않고 보관 상태를 거친다.

## 3. Data Storage Context

### 3.1 영속 저장소

- PostgreSQL
  - 사용자, 워크스페이스, 멤버십, 채널, 메시지, 읽음 상태, 파일 메타데이터 저장

### 3.2 비영속 또는 보조 저장소

- Redis
  - OAuth 로그인 중간 상태
  - 토큰 보조 관리
  - 입력 중 상태
  - Pub/Sub

### 3.3 외부 저장소

- Cloudflare R2
  - 파일 바이너리 저장

## 4. Core Domains

### 4.1 사용자 및 인증 도메인

- 사용자 계정 식별
- 소셜 로그인 공급자 계정 연결 관리
- 워크스페이스가 없는 상태와 새 멤버십 초기화에 사용할 계정 기본
  프로필 관리

### 4.2 워크스페이스 및 멤버십 도메인

- 워크스페이스 생성, 보관, 복원, 삭제
- 워크스페이스별 멤버 역할과 상태 관리
- 워크스페이스별 닉네임과 프로필 이미지 관리
- 초대 링크 기반 참여 관리

### 4.3 채널 도메인

- 공개 채널 및 비공개 채널 관리
- 채널 참여와 접근 제어
- `#general` 기본 채널 보장

### 4.4 메시지 및 읽음 도메인

- 메시지 생성, 수정, 삭제
- 채널 단위 읽음 위치 관리
- 안읽은 메시지 수 계산용 기준 제공

### 4.5 파일 도메인

- 파일 메타데이터 저장
- 메시지 첨부 연결
- 고아 파일 정리 및 보관 만료 정리 지원

## 5. Domain Objects

- User
  - Re-Echo 사용자 계정
- UserIdentity
  - 소셜 로그인 공급자와 사용자 계정 연결
- Workspace
  - 최상위 협업 공간
- WorkspaceMembership
  - 사용자 워크스페이스 소속, 역할, 상태, 워크스페이스별 프로필
- WorkspaceInviteLink
  - 워크스페이스 초대 링크
- Channel
  - 워크스페이스 내부 대화 공간
- ChannelMembership
  - 사용자 채널 참여 상태
- Message
  - 채널 메시지
- ChannelReadState
  - 사용자별 채널 읽음 기준점
- FileObject
  - 외부 스토리지 파일 메타데이터
- MessageAttachment
  - 메시지와 파일 연결

## 6. Entity List

- `users`
- `user_identities`
- `workspaces`
- `workspace_memberships`
- `workspace_invite_links`
- `channels`
- `channel_memberships`
- `messages`
- `channel_read_states`
- `file_objects`
- `message_attachments`

## 7. Entity Details

### 7.1 `users`

설명:
Re-Echo 사용자 기본 계정이다.

| Column | Type | PK | FK | Nullable | Default | Constraints |
| --- | --- | --- | --- | --- | --- | --- |
| id | uuid | Y |  | N |  | 기본 키 |
| display_name | varchar(80) |  |  | N |  | 계정 기본 표시 이름 |
| profile_image_url | text |  |  | Y |  | 계정 기본 프로필 이미지 URL |
| profile_image_file_id | uuid |  | file_objects.id | Y |  | R2 업로드 프로필 이미지 참조 |
| status | varchar(20) |  |  | N | `'ACTIVE'` | `ACTIVE`, `DEACTIVATED` |
| created_at | timestamptz |  |  | N | `now()` | 생성 시각 |
| updated_at | timestamptz |  |  | N | `now()` | 수정 시각 |

### 7.2 `user_identities`

설명:
소셜 로그인 공급자 계정과 사용자 계정의 연결 정보다.

| Column | Type | PK | FK | Nullable | Default | Constraints |
| --- | --- | --- | --- | --- | --- | --- |
| id | uuid | Y |  | N |  | 기본 키 |
| user_id | uuid |  | users.id | N |  | 사용자 참조 |
| provider | varchar(20) |  |  | N |  | `GOOGLE`, `KAKAO`, `GITHUB` |
| provider_user_id | varchar(191) |  |  | N |  | 공급자 사용자 식별자 |
| provider_email | varchar(320) |  |  | Y |  | 공급자 이메일 |
| created_at | timestamptz |  |  | N | `now()` | 생성 시각 |

제약:

- `(provider, provider_user_id)` 유니크
- 하나의 identity는 반드시 하나의 user에 속한다.

### 7.3 `workspaces`

설명:
최상위 협업 공간이다.

| Column | Type | PK | FK | Nullable | Default | Constraints |
| --- | --- | --- | --- | --- | --- | --- |
| id | uuid | Y |  | N |  | 기본 키 |
| name | varchar(100) |  |  | N |  | 워크스페이스 이름 |
| slug | varchar(100) |  |  | Y |  | URL 또는 표시용 식별자 |
| description | varchar(500) |  |  | Y |  | 소개 문구 |
| image_url | text |  |  | Y |  | 워크스페이스 대표 이미지 |
| created_by_user_id | uuid |  | users.id | N |  | 생성자 |
| status | varchar(20) |  |  | N | `'ACTIVE'` | `ACTIVE`, `ARCHIVED`, `DELETED` |
| archived_at | timestamptz |  |  | Y |  | 보관 시각 |
| archive_expires_at | timestamptz |  |  | Y |  | 자동 삭제 예정 시각 |
| deleted_at | timestamptz |  |  | Y |  | 최종 삭제 시각 |
| created_at | timestamptz |  |  | N | `now()` | 생성 시각 |
| updated_at | timestamptz |  |  | N | `now()` | 수정 시각 |

제약:

- `archive_expires_at >= archived_at` when both not null
- `status = 'ARCHIVED'`면 `archived_at`와 `archive_expires_at`가 필요

### 7.4 `workspace_memberships`

설명:
사용자의 워크스페이스 소속, 역할, 상태와 워크스페이스별 프로필을 저장한다.

| Column | Type | PK | FK | Nullable | Default | Constraints |
| --- | --- | --- | --- | --- | --- | --- |
| id | uuid | Y |  | N |  | 기본 키 |
| workspace_id | uuid |  | workspaces.id | N |  | 워크스페이스 참조 |
| user_id | uuid |  | users.id | N |  | 사용자 참조 |
| role | varchar(20) |  |  | N |  | `OWNER`, `ADMIN`, `MEMBER` |
| display_name | varchar(80) |  |  | N |  | 워크스페이스별 표시 이름 |
| profile_image_url | text |  |  | Y |  | 워크스페이스별 프로필 이미지 URL |
| profile_image_file_id | uuid |  | file_objects.id | Y |  | R2 업로드 프로필 이미지 참조 |
| status | varchar(20) |  |  | N | `'ACTIVE'` | `ACTIVE`, `LEFT`, `REMOVED`, `BANNED` |
| joined_at | timestamptz |  |  | N | `now()` | 참여 시각 |
| left_at | timestamptz |  |  | Y |  | 자진 탈퇴 시각 |
| removed_at | timestamptz |  |  | Y |  | 관리자 강제 제거 시각 |
| banned_at | timestamptz |  |  | Y |  | 차단 시각 |
| created_at | timestamptz |  |  | N | `now()` | 생성 시각 |
| updated_at | timestamptz |  |  | N | `now()` | 수정 시각 |

제약:

- `(workspace_id, user_id)` 유니크
- 같은 워크스페이스 내 `display_name` 중복 허용
- `BANNED` 상태면 재가입 불가
- `OWNER`는 워크스페이스당 정확히 1명이어야 하므로 애플리케이션 제약 추가 필요

### 7.5 `workspace_invite_links`

설명:
워크스페이스 참여에 사용하는 초대 링크다.

| Column | Type | PK | FK | Nullable | Default | Constraints |
| --- | --- | --- | --- | --- | --- | --- |
| id | uuid | Y |  | N |  | 기본 키 |
| workspace_id | uuid |  | workspaces.id | N |  | 워크스페이스 참조 |
| token | varchar(100) |  |  | N |  | 초대 토큰 |
| created_by_membership_id | uuid |  | workspace_memberships.id | N |  | 발급 주체 |
| status | varchar(20) |  |  | N | `'ACTIVE'` | `ACTIVE`, `REVOKED`, `EXPIRED` |
| expires_at | timestamptz |  |  | N |  | 만료 시각 |
| revoked_at | timestamptz |  |  | Y |  | 무효화 시각 |
| created_at | timestamptz |  |  | N | `now()` | 생성 시각 |

제약:

- `token` 유니크
- 워크스페이스당 활성 링크는 하나만 허용
- 기본 만료는 생성 후 24시간

### 7.6 `channels`

설명:
워크스페이스 내부 대화 공간이다.

| Column | Type | PK | FK | Nullable | Default | Constraints |
| --- | --- | --- | --- | --- | --- | --- |
| id | uuid | Y |  | N |  | 기본 키 |
| workspace_id | uuid |  | workspaces.id | N |  | 워크스페이스 참조 |
| name | varchar(80) |  |  | N |  | 채널 이름 |
| description | varchar(300) |  |  | Y |  | 채널 설명 |
| visibility | varchar(20) |  |  | N |  | `PUBLIC`, `PRIVATE` |
| is_general | boolean |  |  | N | `false` | 기본 채널 여부 |
| created_by_membership_id | uuid |  | workspace_memberships.id | N |  | 생성 주체 |
| status | varchar(20) |  |  | N | `'ACTIVE'` | `ACTIVE`, `ARCHIVED`, `DELETED` |
| archived_at | timestamptz |  |  | Y |  | 보관 시각 |
| archive_expires_at | timestamptz |  |  | Y |  | 자동 삭제 예정 시각 |
| deleted_at | timestamptz |  |  | Y |  | 최종 삭제 시각 |
| created_at | timestamptz |  |  | N | `now()` | 생성 시각 |
| updated_at | timestamptz |  |  | N | `now()` | 수정 시각 |

제약:

- 같은 워크스페이스 내 `(workspace_id, name)` 유니크
- 워크스페이스당 `is_general = true` 채널은 하나만 허용

### 7.7 `channel_memberships`

설명:
사용자의 채널 참여 상태를 저장한다.

| Column | Type | PK | FK | Nullable | Default | Constraints |
| --- | --- | --- | --- | --- | --- | --- |
| id | uuid | Y |  | N |  | 기본 키 |
| channel_id | uuid |  | channels.id | N |  | 채널 참조 |
| workspace_membership_id | uuid |  | workspace_memberships.id | N |  | 워크스페이스 멤버십 참조 |
| joined_at | timestamptz |  |  | N | `now()` | 참여 시각 |
| left_at | timestamptz |  |  | Y |  | 채널 나가기 시각 |
| status | varchar(20) |  |  | N | `'ACTIVE'` | `ACTIVE`, `LEFT`, `REMOVED` |
| created_at | timestamptz |  |  | N | `now()` | 생성 시각 |
| updated_at | timestamptz |  |  | N | `now()` | 수정 시각 |

제약:

- `(channel_id, workspace_membership_id)` 유니크
- `#general` 채널은 워크스페이스 참여 시 자동 생성 필요

### 7.8 `messages`

설명:
채널에 기록되는 텍스트 메시지다.

| Column | Type | PK | FK | Nullable | Default | Constraints |
| --- | --- | --- | --- | --- | --- | --- |
| id | uuid | Y |  | N |  | 기본 키 |
| channel_id | uuid |  | channels.id | N |  | 채널 참조 |
| author_membership_id | uuid |  | workspace_memberships.id | N |  | 작성자 멤버십 |
| content | text |  |  | N |  | 메시지 본문 |
| status | varchar(20) |  |  | N | `'ACTIVE'` | `ACTIVE`, `DELETED` |
| edited_at | timestamptz |  |  | Y |  | 수정 시각 |
| deleted_at | timestamptz |  |  | Y |  | 삭제 시각 |
| deleted_by_membership_id | uuid |  | workspace_memberships.id | Y |  | 삭제 수행자 |
| created_at | timestamptz |  |  | N | `now()` | 생성 시각 |
| updated_at | timestamptz |  |  | N | `now()` | 수정 시각 |

제약:

- 빈 문자열 메시지 금지
- 첨부만 있는 메시지 허용

### 7.9 `channel_read_states`

설명:
사용자별 채널 읽음 기준점을 저장한다.

| Column | Type | PK | FK | Nullable | Default | Constraints |
| --- | --- | --- | --- | --- | --- | --- |
| id | uuid | Y |  | N |  | 기본 키 |
| channel_membership_id | uuid |  | channel_memberships.id | N |  | 채널 멤버십 참조 |
| last_read_message_id | uuid |  | messages.id | Y |  | 마지막 읽은 메시지 |
| last_read_at | timestamptz |  |  | Y |  | 마지막 읽음 시각 |
| created_at | timestamptz |  |  | N | `now()` | 생성 시각 |
| updated_at | timestamptz |  |  | N | `now()` | 수정 시각 |

제약:

- `channel_membership_id` 유니크

### 7.10 `file_objects`

설명:
외부 오브젝트 스토리지에 저장된 파일 메타데이터다.

| Column | Type | PK | FK | Nullable | Default | Constraints |
| --- | --- | --- | --- | --- | --- | --- |
| id | uuid | Y |  | N |  | 기본 키 |
| workspace_id | uuid |  | workspaces.id | Y |  | 워크스페이스 참조 |
| uploaded_by_user_id | uuid |  | users.id | N |  | 업로더 사용자 |
| uploaded_by_membership_id | uuid |  | workspace_memberships.id | Y |  | 워크스페이스 문맥 업로더 |
| purpose | varchar(30) |  |  | N |  | `MESSAGE_ATTACHMENT`, `PROFILE_IMAGE` |
| storage_provider | varchar(30) |  |  | N |  | 기본값 `R2` |
| storage_key | varchar(255) |  |  | N |  | 저장소 내부 키 |
| original_filename | varchar(255) |  |  | N |  | 원본 파일명 |
| content_type | varchar(120) |  |  | N |  | MIME 타입 |
| file_size_bytes | bigint |  |  | N |  | 메시지 첨부 최대 20MB, 프로필 이미지 최대 10MB |
| image_width | integer |  |  | Y |  | 이미지 가로 크기 |
| image_height | integer |  |  | Y |  | 이미지 세로 크기 |
| status | varchar(20) |  |  | N | `'ACTIVE'` | `ACTIVE`, `ORPHANED`, `DELETED` |
| uploaded_at | timestamptz |  |  | N | `now()` | 업로드 시각 |
| deleted_at | timestamptz |  |  | Y |  | 삭제 시각 |
| created_at | timestamptz |  |  | N | `now()` | 생성 시각 |

제약:

- `storage_key` 유니크
- `purpose = 'MESSAGE_ATTACHMENT'`이면 `workspace_id`와
  `uploaded_by_membership_id`가 필요하다.
- `purpose = 'PROFILE_IMAGE'`이면 이미지 MIME 타입만 허용하고
  `file_size_bytes <= 10485760`이어야 한다.
- `purpose = 'MESSAGE_ATTACHMENT'`이면 `file_size_bytes <= 20971520`

### 7.11 `message_attachments`

설명:
메시지와 파일의 연결 관계다.

| Column | Type | PK | FK | Nullable | Default | Constraints |
| --- | --- | --- | --- | --- | --- | --- |
| id | uuid | Y |  | N |  | 기본 키 |
| message_id | uuid |  | messages.id | N |  | 메시지 참조 |
| file_object_id | uuid |  | file_objects.id | N |  | 파일 참조 |
| sort_order | integer |  |  | N | `0` | 메시지 내 표시 순서 |
| created_at | timestamptz |  |  | N | `now()` | 생성 시각 |

제약:

- `(message_id, file_object_id)` 유니크
- `sort_order >= 0`

## 8. Relationship Definitions

- `users` 1:N `user_identities`
- `users` 1:N `workspaces`
  - 생성자 기준 관계
- `users` 1:N `workspace_memberships`
- `users` 1:N `file_objects`
  - 업로더 사용자 기준 관계
- `file_objects` 1:N `users`
  - 계정 기본 프로필 이미지 참조 기준 관계
- `workspaces` 1:N `workspace_memberships`
- `workspaces` 1:N `workspace_invite_links`
- `workspaces` 1:N `channels`
- `workspace_memberships` 1:N `workspace_invite_links`
  - 발급자 기준 관계
- `workspace_memberships` 1:N `channel_memberships`
- `channels` 1:N `channel_memberships`
- `channels` 1:N `messages`
- `workspace_memberships` 1:N `messages`
  - 작성자 기준 관계
- `channel_memberships` 1:1 `channel_read_states`
- `messages` 1:N `message_attachments`
- `file_objects` 1:N `message_attachments`
- `workspaces` 1:N `file_objects`
- `workspace_memberships` 1:N `file_objects`
  - 업로더 기준 관계
- `file_objects` 1:N `workspace_memberships`
  - 워크스페이스별 프로필 이미지 참조 기준 관계

관계 원칙:

- N:M 관계는 모두 중간 엔티티로 분리한다.
- 채널 참여는 사용자 계정이 아니라 `workspace_membership`을 기준으로 연결한다.
- 메시지 작성, 초대 링크 발급, 메시지 첨부 파일 업로드는 모두
  워크스페이스 문맥을 가진 멤버십 FK로 추적한다.
- 계정 기본 프로필 이미지 업로드는 워크스페이스가 없을 수 있으므로
  사용자 FK로 추적한다.
- 계정 기본 표시 이름과 프로필 이미지는 `users`에 저장한다.
- 워크스페이스 생성·참여 시 계정 기본 프로필을 멤버십 프로필로 복사한다.
- 복사된 워크스페이스별 닉네임과 프로필 이미지는
  `workspace_memberships`에서 독립적으로 관리한다.

## 9. Status & Enum Definitions

### 9.1 User Status

- `ACTIVE`
- `DEACTIVATED`

### 9.2 Workspace Membership Role

- `OWNER`
- `ADMIN`
- `MEMBER`

### 9.3 Workspace Membership Status

- `ACTIVE`
- `LEFT`
- `REMOVED`
- `BANNED`

### 9.4 Invite Link Status

- `ACTIVE`
- `REVOKED`
- `EXPIRED`

### 9.5 Channel Visibility

- `PUBLIC`
- `PRIVATE`

### 9.6 Channel Status

- `ACTIVE`
- `ARCHIVED`
- `DELETED`

### 9.7 Channel Membership Status

- `ACTIVE`
- `LEFT`
- `REMOVED`

### 9.8 Message Status

- `ACTIVE`
- `DELETED`

### 9.9 File Status

- `ACTIVE`
- `ORPHANED`
- `DELETED`

### 9.10 File Purpose

- `MESSAGE_ATTACHMENT`
- `PROFILE_IMAGE`

## 10. Deletion / Archive / Expiration / History Policy

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
- 차단은 `BANNED`

### 10.4 메시지

- 메시지는 소프트 삭제를 기본으로 한다
- 삭제자는 `deleted_by_membership_id`로 추적한다
- 수정 이력 테이블은 MVP 범위에 포함하지 않는다

### 10.5 파일

- 파일 메타데이터 등록 실패 시 `ORPHANED` 상태가 될 수 있다
- 정기 스케줄러가 고아 파일과 만료 대상 파일을 정리한다

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

- `workspace_memberships(user_id, status)`
  - 사용자가 속한 워크스페이스 목록 조회
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
