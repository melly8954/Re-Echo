CREATE TABLE users (
    id uuid PRIMARY KEY,
    display_name varchar(80) NOT NULL,
    profile_image_url text,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_users_status
        CHECK (status IN ('ACTIVE', 'DEACTIVATED'))
);

CREATE TABLE user_identities (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    provider varchar(20) NOT NULL,
    provider_user_id varchar(191) NOT NULL,
    provider_email varchar(320),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_user_identities_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_user_identities_provider
        CHECK (provider IN ('GOOGLE', 'KAKAO', 'GITHUB')),
    CONSTRAINT uq_user_identities_provider_user
        UNIQUE (provider, provider_user_id)
);

CREATE TABLE workspaces (
    id uuid PRIMARY KEY,
    name varchar(100) NOT NULL,
    slug varchar(100),
    description varchar(500),
    image_url text,
    created_by_user_id uuid NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    archived_at timestamptz,
    archive_expires_at timestamptz,
    deleted_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_workspaces_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT chk_workspaces_status
        CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED')),
    CONSTRAINT chk_workspaces_archive_range
        CHECK (archive_expires_at IS NULL OR archived_at IS NOT NULL),
    CONSTRAINT chk_workspaces_archive_expires_at
        CHECK (archive_expires_at IS NULL OR archive_expires_at >= archived_at),
    CONSTRAINT chk_workspaces_archived_required_fields
        CHECK (
            status <> 'ARCHIVED'
            OR (archived_at IS NOT NULL AND archive_expires_at IS NOT NULL)
        )
);

CREATE TABLE workspace_memberships (
    id uuid PRIMARY KEY,
    workspace_id uuid NOT NULL,
    user_id uuid NOT NULL,
    role varchar(20) NOT NULL,
    display_name varchar(80) NOT NULL,
    profile_image_url text,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    joined_at timestamptz NOT NULL DEFAULT now(),
    left_at timestamptz,
    removed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_workspace_memberships_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id) ON DELETE CASCADE,
    CONSTRAINT fk_workspace_memberships_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_workspace_memberships_role
        CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    CONSTRAINT chk_workspace_memberships_status
        CHECK (status IN ('ACTIVE', 'LEFT', 'REMOVED')),
    CONSTRAINT uq_workspace_memberships_workspace_user
        UNIQUE (workspace_id, user_id)
);

CREATE TABLE workspace_invite_links (
    id uuid PRIMARY KEY,
    workspace_id uuid NOT NULL,
    token varchar(100) NOT NULL,
    created_by_membership_id uuid NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_workspace_invite_links_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id) ON DELETE CASCADE,
    CONSTRAINT fk_workspace_invite_links_created_by_membership
        FOREIGN KEY (created_by_membership_id) REFERENCES workspace_memberships (id),
    CONSTRAINT chk_workspace_invite_links_status
        CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED')),
    CONSTRAINT chk_workspace_invite_links_expires_at
        CHECK (expires_at > created_at),
    CONSTRAINT uq_workspace_invite_links_token
        UNIQUE (token)
);

CREATE TABLE channels (
    id uuid PRIMARY KEY,
    workspace_id uuid NOT NULL,
    name varchar(80) NOT NULL,
    description varchar(300),
    visibility varchar(20) NOT NULL,
    is_general boolean NOT NULL DEFAULT false,
    created_by_membership_id uuid NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    archived_at timestamptz,
    archive_expires_at timestamptz,
    deleted_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_channels_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id) ON DELETE CASCADE,
    CONSTRAINT fk_channels_created_by_membership
        FOREIGN KEY (created_by_membership_id) REFERENCES workspace_memberships (id),
    CONSTRAINT chk_channels_visibility
        CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT chk_channels_status
        CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED')),
    CONSTRAINT chk_channels_archive_range
        CHECK (archive_expires_at IS NULL OR archived_at IS NOT NULL),
    CONSTRAINT chk_channels_archive_expires_at
        CHECK (archive_expires_at IS NULL OR archive_expires_at >= archived_at),
    CONSTRAINT chk_channels_archived_required_fields
        CHECK (
            status <> 'ARCHIVED'
            OR (archived_at IS NOT NULL AND archive_expires_at IS NOT NULL)
        ),
    CONSTRAINT uq_channels_workspace_name
        UNIQUE (workspace_id, name)
);

CREATE TABLE channel_memberships (
    id uuid PRIMARY KEY,
    channel_id uuid NOT NULL,
    workspace_membership_id uuid NOT NULL,
    joined_at timestamptz NOT NULL DEFAULT now(),
    left_at timestamptz,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_channel_memberships_channel
        FOREIGN KEY (channel_id) REFERENCES channels (id) ON DELETE CASCADE,
    CONSTRAINT fk_channel_memberships_workspace_membership
        FOREIGN KEY (workspace_membership_id)
        REFERENCES workspace_memberships (id) ON DELETE CASCADE,
    CONSTRAINT chk_channel_memberships_status
        CHECK (status IN ('ACTIVE', 'LEFT', 'REMOVED')),
    CONSTRAINT uq_channel_memberships_channel_workspace_membership
        UNIQUE (channel_id, workspace_membership_id)
);

CREATE TABLE messages (
    id uuid PRIMARY KEY,
    channel_id uuid NOT NULL,
    author_membership_id uuid NOT NULL,
    content text NOT NULL DEFAULT '',
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    edited_at timestamptz,
    deleted_at timestamptz,
    deleted_by_membership_id uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_messages_channel
        FOREIGN KEY (channel_id) REFERENCES channels (id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_author_membership
        FOREIGN KEY (author_membership_id) REFERENCES workspace_memberships (id),
    CONSTRAINT fk_messages_deleted_by_membership
        FOREIGN KEY (deleted_by_membership_id)
        REFERENCES workspace_memberships (id) ON DELETE SET NULL,
    CONSTRAINT chk_messages_status
        CHECK (status IN ('ACTIVE', 'DELETED'))
);

CREATE TABLE channel_read_states (
    id uuid PRIMARY KEY,
    channel_membership_id uuid NOT NULL,
    last_read_message_id uuid,
    last_read_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_channel_read_states_channel_membership
        FOREIGN KEY (channel_membership_id)
        REFERENCES channel_memberships (id) ON DELETE CASCADE,
    CONSTRAINT fk_channel_read_states_last_read_message
        FOREIGN KEY (last_read_message_id) REFERENCES messages (id) ON DELETE SET NULL,
    CONSTRAINT uq_channel_read_states_channel_membership
        UNIQUE (channel_membership_id)
);

CREATE TABLE file_objects (
    id uuid PRIMARY KEY,
    workspace_id uuid NOT NULL,
    uploaded_by_membership_id uuid NOT NULL,
    storage_provider varchar(30) NOT NULL DEFAULT 'R2',
    storage_key varchar(255) NOT NULL,
    original_filename varchar(255) NOT NULL,
    content_type varchar(120) NOT NULL,
    file_size_bytes bigint NOT NULL,
    image_width integer,
    image_height integer,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    uploaded_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_file_objects_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id) ON DELETE CASCADE,
    CONSTRAINT fk_file_objects_uploaded_by_membership
        FOREIGN KEY (uploaded_by_membership_id) REFERENCES workspace_memberships (id),
    CONSTRAINT chk_file_objects_storage_provider
        CHECK (storage_provider IN ('R2')),
    CONSTRAINT chk_file_objects_file_size
        CHECK (file_size_bytes > 0 AND file_size_bytes <= 20971520),
    CONSTRAINT chk_file_objects_image_width
        CHECK (image_width IS NULL OR image_width >= 0),
    CONSTRAINT chk_file_objects_image_height
        CHECK (image_height IS NULL OR image_height >= 0),
    CONSTRAINT chk_file_objects_status
        CHECK (status IN ('ACTIVE', 'ORPHANED', 'DELETED')),
    CONSTRAINT uq_file_objects_storage_key
        UNIQUE (storage_key)
);

CREATE TABLE message_attachments (
    id uuid PRIMARY KEY,
    message_id uuid NOT NULL,
    file_object_id uuid NOT NULL,
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_message_attachments_message
        FOREIGN KEY (message_id) REFERENCES messages (id) ON DELETE CASCADE,
    CONSTRAINT fk_message_attachments_file_object
        FOREIGN KEY (file_object_id) REFERENCES file_objects (id),
    CONSTRAINT chk_message_attachments_sort_order
        CHECK (sort_order >= 0),
    CONSTRAINT uq_message_attachments_message_file_object
        UNIQUE (message_id, file_object_id)
);

CREATE UNIQUE INDEX uq_workspace_memberships_active_owner
    ON workspace_memberships (workspace_id)
    WHERE role = 'OWNER' AND status = 'ACTIVE';

CREATE UNIQUE INDEX uq_workspace_invite_links_active_workspace
    ON workspace_invite_links (workspace_id)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX uq_channels_general_workspace
    ON channels (workspace_id)
    WHERE is_general = true;

CREATE INDEX idx_user_identities_user_id
    ON user_identities (user_id);

CREATE INDEX idx_workspaces_created_by_user_id
    ON workspaces (created_by_user_id);

CREATE INDEX idx_workspace_memberships_user_status
    ON workspace_memberships (user_id, status);

CREATE INDEX idx_workspace_memberships_workspace_display_name
    ON workspace_memberships (workspace_id, display_name);

CREATE INDEX idx_workspace_invite_links_workspace_status
    ON workspace_invite_links (workspace_id, status);

CREATE INDEX idx_channels_workspace_status_visibility
    ON channels (workspace_id, status, visibility);

CREATE INDEX idx_channel_memberships_workspace_membership_status
    ON channel_memberships (workspace_membership_id, status);

CREATE INDEX idx_messages_channel_created_id
    ON messages (channel_id, created_at DESC, id DESC);

CREATE INDEX idx_messages_author_membership_id
    ON messages (author_membership_id);

CREATE INDEX idx_file_objects_workspace_status_uploaded_at
    ON file_objects (workspace_id, status, uploaded_at DESC);

CREATE INDEX idx_file_objects_uploaded_by_membership_id
    ON file_objects (uploaded_by_membership_id);
