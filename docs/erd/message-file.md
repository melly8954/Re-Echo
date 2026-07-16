# Re-Echo 1차 MVP ERD: 메시지와 파일

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
| purpose | varchar(30) |  |  | N |  | `MESSAGE_ATTACHMENT`, `PROFILE_IMAGE`, `WORKSPACE_IMAGE` |
| storage_provider | varchar(30) |  |  | N |  | 기본값 `R2` |
| storage_key | varchar(255) |  |  | N |  | 저장소 내부 키 |
| original_filename | varchar(255) |  |  | N |  | 원본 파일명 |
| content_type | varchar(120) |  |  | N |  | MIME 타입 |
| file_size_bytes | bigint |  |  | N |  | 메시지 첨부 최대 20MB, 프로필·대표 이미지는 최대 10MB |
| image_width | integer |  |  | Y |  | 이미지 가로 크기 |
| image_height | integer |  |  | Y |  | 이미지 세로 크기 |
| status | varchar(20) |  |  | N | `'ACTIVE'` | `ACTIVE`, `ORPHANED`, `DELETED` |
| uploaded_at | timestamptz |  |  | N | `now()` | 업로드 시각 |
| orphaned_at | timestamptz |  |  | Y |  | 미연결 상태 전환 시각 |
| deleted_at | timestamptz |  |  | Y |  | 삭제 시각 |
| created_at | timestamptz |  |  | N | `now()` | 생성 시각 |

제약:

- `storage_key` 유니크
- `purpose = 'MESSAGE_ATTACHMENT'`이면 `workspace_id`와
  `uploaded_by_membership_id`가 필요하다.
- `purpose = 'PROFILE_IMAGE'`이면 이미지 MIME 타입만 허용하고
  `file_size_bytes <= 10485760`이어야 한다.
- `purpose = 'PROFILE_IMAGE'`인 파일은 계정 기본 프로필 이미지와
  워크스페이스 프로필 이미지 문맥을 구분한다.
  - 계정 기본 프로필 이미지는 `workspace_id`와
    `uploaded_by_membership_id`가 모두 `null`이다.
   - 워크스페이스 프로필 이미지는 `workspace_id`와
     `uploaded_by_membership_id`가 모두 필요하다.
- `purpose = 'WORKSPACE_IMAGE'`이면 이미지 MIME 타입만 허용하고
  `file_size_bytes <= 10485760`이어야 한다. `workspace_id`와
  `uploaded_by_membership_id`가 모두 필요하며, 해당 워크스페이스의
  `image_file_id`로만 최종 연결할 수 있다.
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
