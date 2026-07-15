package com.reecho.reechobe.file.repository;

import com.reecho.reechobe.file.domain.FileObject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileObjectRepository extends JpaRepository<FileObject, UUID> {

    @Query(
            value = """
                    SELECT fo.*
                    FROM file_objects fo
                    WHERE fo.purpose IN ('PROFILE_IMAGE', 'WORKSPACE_IMAGE')
                      AND fo.status IN ('ACTIVE', 'ORPHANED')
                      AND (
                          (fo.status = 'ORPHANED'
                              AND fo.orphaned_at IS NOT NULL
                              AND fo.orphaned_at < :cutoff)
                          OR (fo.status = 'ACTIVE'
                              AND fo.created_at < :cutoff)
                      )
                      AND NOT EXISTS (
                          SELECT 1
                          FROM users u
                          WHERE u.profile_image_file_id = fo.id
                      )
                      AND NOT EXISTS (
                          SELECT 1
                          FROM workspace_memberships wm
                          WHERE wm.profile_image_file_id = fo.id
                      )
                      AND NOT EXISTS (
                          SELECT 1
                          FROM workspaces w
                          WHERE w.image_file_id = fo.id
                      )
                    ORDER BY fo.created_at ASC
                    """,
            nativeQuery = true
    )
    List<FileObject> findProfileImageCleanupCandidates(
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM users u
                        WHERE u.profile_image_file_id = :fileId
                        UNION ALL
                        SELECT 1
                        FROM workspace_memberships wm
                        WHERE wm.profile_image_file_id = :fileId
                        UNION ALL
                        SELECT 1
                        FROM workspaces w
                        WHERE w.image_file_id = :fileId
                    )
                    """,
            nativeQuery = true
    )
    boolean existsProfileImageReference(@Param("fileId") UUID fileId);

    @Query(
            value = """
                    SELECT fo.*
                    FROM file_objects fo
                    WHERE fo.purpose = 'MESSAGE_ATTACHMENT'
                      AND fo.status = 'ACTIVE'
                      AND fo.created_at < :cutoff
                      AND NOT EXISTS (
                          SELECT 1
                          FROM message_attachments ma
                          WHERE ma.file_object_id = fo.id
                      )
                    ORDER BY fo.created_at ASC
                    """,
            nativeQuery = true
    )
    List<FileObject> findUnattachedMessageCleanupCandidates(
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM message_attachments ma
                        WHERE ma.file_object_id = :fileId
                    )
                    """,
            nativeQuery = true
    )
    boolean existsMessageAttachmentReference(@Param("fileId") UUID fileId);
}
