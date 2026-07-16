# Re-Echo 1차 MVP ERD: 사용자와 인증

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
