ALTER TABLE file_objects
    ADD COLUMN uploaded_by_user_id uuid;

ALTER TABLE file_objects
    ADD COLUMN purpose varchar(30) NOT NULL DEFAULT 'MESSAGE_ATTACHMENT';

UPDATE file_objects fo
SET uploaded_by_user_id = wm.user_id
FROM workspace_memberships wm
WHERE fo.uploaded_by_membership_id = wm.id
  AND fo.uploaded_by_user_id IS NULL;

ALTER TABLE file_objects
    ALTER COLUMN uploaded_by_user_id SET NOT NULL;

ALTER TABLE file_objects
    ALTER COLUMN workspace_id DROP NOT NULL;

ALTER TABLE file_objects
    ALTER COLUMN uploaded_by_membership_id DROP NOT NULL;

ALTER TABLE file_objects
    DROP CONSTRAINT chk_file_objects_file_size;

ALTER TABLE file_objects
    ADD CONSTRAINT fk_file_objects_uploaded_by_user
        FOREIGN KEY (uploaded_by_user_id) REFERENCES users (id);

ALTER TABLE file_objects
    ADD CONSTRAINT chk_file_objects_purpose
        CHECK (purpose IN ('MESSAGE_ATTACHMENT', 'PROFILE_IMAGE'));

ALTER TABLE file_objects
    ADD CONSTRAINT chk_file_objects_context
        CHECK (
            (purpose = 'MESSAGE_ATTACHMENT'
                AND workspace_id IS NOT NULL
                AND uploaded_by_membership_id IS NOT NULL)
            OR purpose = 'PROFILE_IMAGE'
        );

ALTER TABLE file_objects
    ADD CONSTRAINT chk_file_objects_file_size_by_purpose
        CHECK (
            file_size_bytes > 0
            AND (
                (purpose = 'MESSAGE_ATTACHMENT' AND file_size_bytes <= 20971520)
                OR (purpose = 'PROFILE_IMAGE' AND file_size_bytes <= 10485760)
            )
        );

ALTER TABLE file_objects
    ADD CONSTRAINT chk_file_objects_profile_image_content_type
        CHECK (
            purpose <> 'PROFILE_IMAGE'
            OR content_type IN ('image/jpeg', 'image/png', 'image/webp')
        );

CREATE INDEX idx_file_objects_uploaded_by_user_id
    ON file_objects (uploaded_by_user_id);

CREATE INDEX idx_file_objects_uploaded_by_user_purpose_created_at
    ON file_objects (uploaded_by_user_id, purpose, created_at DESC);

ALTER TABLE users
    ADD COLUMN profile_image_file_id uuid;

ALTER TABLE users
    ADD CONSTRAINT fk_users_profile_image_file
        FOREIGN KEY (profile_image_file_id)
        REFERENCES file_objects (id)
        ON DELETE SET NULL;

ALTER TABLE workspace_memberships
    ADD COLUMN profile_image_file_id uuid;

ALTER TABLE workspace_memberships
    ADD CONSTRAINT fk_workspace_memberships_profile_image_file
        FOREIGN KEY (profile_image_file_id)
        REFERENCES file_objects (id)
        ON DELETE SET NULL;
