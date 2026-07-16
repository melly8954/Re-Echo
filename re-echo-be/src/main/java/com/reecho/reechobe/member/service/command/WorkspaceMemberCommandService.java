package com.reecho.reechobe.member.service.command;

import com.reecho.reechobe.channel.domain.ChannelMembership;
import com.reecho.reechobe.channel.domain.ChannelMembershipStatus;
import com.reecho.reechobe.channel.repository.ChannelMembershipRepository;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.common.exception.CommonErrorCode;
import com.reecho.reechobe.file.dto.PresignedUploadResponse;
import com.reecho.reechobe.file.dto.ProfileImagePresignRequest;
import com.reecho.reechobe.file.service.ProfileImageFileService;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.dto.UpdateWorkspaceProfileRequest;
import com.reecho.reechobe.member.dto.WorkspaceMemberResponse;
import com.reecho.reechobe.member.exception.MemberErrorCode;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 워크스페이스 멤버십의 역할과 참여 상태 변경을 처리한다.
@Service
@RequiredArgsConstructor
public class WorkspaceMemberCommandService {

    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final ChannelMembershipRepository channelMembershipRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ProfileImageFileService profileImageFileService;

    @Transactional
    // 현재 멤버만 자신의 워크스페이스 프로필을 계정 기본 프로필과 분리해 수정한다.
    public WorkspaceMemberResponse updateMyProfile(
            UUID userId,
            UUID workspaceId,
            UpdateWorkspaceProfileRequest request
    ) {
        WorkspaceMembership membership = getActiveEditableMembership(userId, workspaceId);
        UUID previousProfileImageFileId = membership.getProfileImageFileId();
        membership.updateDisplayName(request.displayName());
        if (request.profileImageFileIdPresent()) {
            updateProfileImage(userId, workspaceId, membership, previousProfileImageFileId, request);
        }
        return WorkspaceMemberResponse.from(membership);
    }

    @Transactional
    // 현재 멤버가 자신의 워크스페이스 프로필용 이미지를 직접 업로드하도록 준비한다.
    public PresignedUploadResponse createMyProfileImageUpload(
            UUID userId,
            UUID workspaceId,
            ProfileImagePresignRequest request
    ) {
        WorkspaceMembership membership = getActiveEditableMembership(userId, workspaceId);
        return profileImageFileService.createWorkspaceProfileImageUpload(
                workspaceId,
                userId,
                membership.getId(),
                request
        );
    }

    @Transactional
    // 본인만 워크스페이스를 탈퇴하고 연결된 채널 참여 상태도 함께 해제한다.
    public void leaveWorkspace(UUID userId, UUID workspaceId, UUID memberId) {
        WorkspaceMembership membership = getActiveMembership(userId, workspaceId);
        if (!membership.getId().equals(memberId)) {
            throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
        }
        if (membership.getRole() == WorkspaceMembershipRole.OWNER) {
            throw new BusinessException(MemberErrorCode.MEMBER_LAST_OWNER_LEAVE_FORBIDDEN);
        }

        membership.leave();
        leaveActiveChannelMemberships(membership.getId());
    }

    @Transactional
    // 소유자가 다른 활성 멤버의 관리자 또는 일반 멤버 역할을 변경한다.
    public void changeMemberRole(
            UUID userId,
            UUID workspaceId,
            UUID memberId,
            WorkspaceMembershipRole role
    ) {
        WorkspaceMembership actorMembership = getActiveMembership(userId, workspaceId);
        if (actorMembership.getRole() != WorkspaceMembershipRole.OWNER) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED);
        }
        if (role == WorkspaceMembershipRole.OWNER) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        WorkspaceMembership targetMembership = getActiveMembershipById(memberId, workspaceId);
        if (targetMembership.getRole() == WorkspaceMembershipRole.OWNER) {
            throw new BusinessException(MemberErrorCode.MEMBER_LAST_OWNER_CHANGE_FORBIDDEN);
        }

        targetMembership.changeRole(role);
    }

    @Transactional
    // 소유자 또는 관리자가 대상 멤버와 해당 채널 접근 권한을 제거한다.
    public void removeMember(UUID userId, UUID workspaceId, UUID memberId) {
        WorkspaceMembership actorMembership = getActiveMembership(userId, workspaceId);
        if (actorMembership.getRole() == WorkspaceMembershipRole.MEMBER) {
            throw new BusinessException(MemberErrorCode.MEMBER_REMOVE_FORBIDDEN);
        }

        WorkspaceMembership targetMembership = getActiveMembershipById(memberId, workspaceId);
        if (targetMembership.getRole() == WorkspaceMembershipRole.OWNER) {
            throw new BusinessException(MemberErrorCode.MEMBER_REMOVE_FORBIDDEN);
        }

        targetMembership.remove();
        leaveActiveChannelMemberships(targetMembership.getId());
    }

    private WorkspaceMembership getActiveMembership(UUID userId, UUID workspaceId) {
        return workspaceMembershipRepository
                .findByWorkspaceIdAndUserIdAndStatus(
                        workspaceId,
                        userId,
                        WorkspaceMembershipStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
    }

    // 보관 또는 삭제된 워크스페이스에서는 프로필 업로드와 변경을 허용하지 않는다.
    private WorkspaceMembership getActiveEditableMembership(UUID userId, UUID workspaceId) {
        WorkspaceStatus workspaceStatus = workspaceRepository.findById(workspaceId)
                .map(workspace -> workspace.getStatus())
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        if (workspaceStatus == WorkspaceStatus.DELETED) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
        }
        if (workspaceStatus == WorkspaceStatus.ARCHIVED) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_ARCHIVED);
        }
        return getActiveMembership(userId, workspaceId);
    }

    // 새 파일은 현재 멤버십 문맥을 검증한 뒤 연결하고 이전 전용 파일만 정리한다.
    private void updateProfileImage(
            UUID userId,
            UUID workspaceId,
            WorkspaceMembership membership,
            UUID previousProfileImageFileId,
            UpdateWorkspaceProfileRequest request
    ) {
        if (request.profileImageFileId() == null) {
            membership.removeProfileImage();
            markPreviousWorkspaceProfileImageOrphaned(
                    workspaceId,
                    membership.getId(),
                    previousProfileImageFileId,
                    null
            );
            return;
        }
        String profileImageUrl = profileImageFileService.requireUploadedWorkspaceProfileImageUrl(
                workspaceId,
                userId,
                membership.getId(),
                request.profileImageFileId()
        );
        markPreviousWorkspaceProfileImageOrphaned(
                workspaceId,
                membership.getId(),
                previousProfileImageFileId,
                request.profileImageFileId()
        );
        membership.updateProfileImage(profileImageUrl, request.profileImageFileId());
    }

    // 초기값으로 복사된 계정 이미지는 소유 문맥이 다르므로 고아 처리하지 않는다.
    private void markPreviousWorkspaceProfileImageOrphaned(
            UUID workspaceId,
            UUID membershipId,
            UUID previousProfileImageFileId,
            UUID nextProfileImageFileId
    ) {
        if (previousProfileImageFileId == null || previousProfileImageFileId.equals(nextProfileImageFileId)) {
            return;
        }
        profileImageFileService.markWorkspaceProfileImageOrphaned(
                workspaceId,
                membershipId,
                previousProfileImageFileId
        );
    }

    private WorkspaceMembership getActiveMembershipById(UUID memberId, UUID workspaceId) {
        return workspaceMembershipRepository
                .findByIdAndWorkspaceIdAndStatus(
                        memberId,
                        workspaceId,
                        WorkspaceMembershipStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    // 재참여 시 기본 채널만 자동 복구되도록 기존 채널 참여도 함께 종료한다.
    // 워크스페이스 접근이 끝난 멤버가 기존 채널에 남지 않도록 모든 활성 참여를 해제한다.
    private void leaveActiveChannelMemberships(UUID workspaceMembershipId) {
        channelMembershipRepository
                .findByWorkspaceMembershipIdAndStatus(
                        workspaceMembershipId,
                        ChannelMembershipStatus.ACTIVE
                )
                .forEach(ChannelMembership::leave);
    }
}
