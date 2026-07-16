ALTER TABLE file_objects
    DROP CONSTRAINT chk_file_objects_purpose;

ALTER TABLE file_objects
    ADD CONSTRAINT chk_file_objects_purpose
        CHECK (purpose IN ('MESSAGE_ATTACHMENT', 'PROFILE_IMAGE', 'WORKSPACE_IMAGE'));

ALTER TABLE file_objects
    DROP CONSTRAINT chk_file_objects_context;

ALTER TABLE file_objects
    ADD CONSTRAINT chk_file_objects_context
        CHECK (
            (purpose = 'MESSAGE_ATTACHMENT'
                AND workspace_id IS NOT NULL
                AND uploaded_by_membership_id IS NOT NULL)
            OR (purpose = 'PROFILE_IMAGE')
            OR (purpose = 'WORKSPACE_IMAGE'
                AND workspace_id IS NOT NULL
                AND uploaded_by_membership_id IS NOT NULL)
        );

ALTER TABLE file_objects
    DROP CONSTRAINT chk_file_objects_file_size_by_purpose;

ALTER TABLE file_objects
    ADD CONSTRAINT chk_file_objects_file_size_by_purpose
        CHECK (
            file_size_bytes > 0
            AND (
                (purpose = 'MESSAGE_ATTACHMENT' AND file_size_bytes <= 20971520)
                OR (purpose IN ('PROFILE_IMAGE', 'WORKSPACE_IMAGE')
                    AND file_size_bytes <= 10485760)
            )
        );

ALTER TABLE file_objects
    ADD CONSTRAINT chk_file_objects_workspace_image_content_type
        CHECK (
            purpose <> 'WORKSPACE_IMAGE'
            OR content_type IN ('image/jpeg', 'image/png', 'image/webp')
        );

ALTER TABLE workspaces
    ADD COLUMN image_file_id uuid;

ALTER TABLE workspaces
    ADD CONSTRAINT fk_workspaces_image_file
        FOREIGN KEY (image_file_id)
        REFERENCES file_objects (id)
        ON DELETE SET NULL;

CREATE INDEX idx_workspaces_image_file_id
    ON workspaces (image_file_id);
