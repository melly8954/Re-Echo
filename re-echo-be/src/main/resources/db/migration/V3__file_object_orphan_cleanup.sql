ALTER TABLE file_objects
    ADD COLUMN orphaned_at timestamptz;

CREATE INDEX idx_file_objects_purpose_status_orphaned_at
    ON file_objects (purpose, status, orphaned_at);
