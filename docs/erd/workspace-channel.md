# Re-Echo 1차 MVP ERD: 워크스페이스와 채널

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
| image_file_id | uuid |  | file_objects.id | Y |  | R2 업로드 대표 이미지 참조 |
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
| status | varchar(20) |  |  | N | `'ACTIVE'` | `ACTIVE`, `LEFT`, `REMOVED` |
| joined_at | timestamptz |  |  | N | `now()` | 참여 시각 |
| last_visited_at | timestamptz |  |  | N | `now()` | 마지막 워크스페이스 진입 시각 |
| left_at | timestamptz |  |  | Y |  | 자진 탈퇴 시각 |
| removed_at | timestamptz |  |  | Y |  | 관리자 강제 제거 시각 |
| created_at | timestamptz |  |  | N | `now()` | 생성 시각 |
| updated_at | timestamptz |  |  | N | `now()` | 수정 시각 |

제약:

- `(workspace_id, user_id)` 유니크
- 같은 워크스페이스 내 `display_name` 중복 허용
- `LEFT` 상태는 재참여 시 기존 행을 `ACTIVE`로 복구
- `REMOVED` 상태면 재가입 불가
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
