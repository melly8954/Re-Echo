package com.reecho.reechobe.invite.service.command;

import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.channel.domain.Channel;
import com.reecho.reechobe.channel.domain.ChannelMembership;
import com.reecho.reechobe.channel.repository.ChannelMembershipRepository;
import com.reecho.reechobe.channel.repository.ChannelRepository;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.invite.domain.WorkspaceInviteLink;
import com.reecho.reechobe.invite.domain.WorkspaceInviteLinkStatus;
import com.reecho.reechobe.invite.dto.JoinedWorkspaceResponse;
import com.reecho.reechobe.invite.dto.WorkspaceInviteLinkResponse;
import com.reecho.reechobe.invite.exception.InviteErrorCode;
import com.reecho.reechobe.invite.repository.WorkspaceInviteLinkRepository;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.exception.MemberErrorCode;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.user.domain.User;
import com.reecho.reechobe.user.repository.UserRepository;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 초대 링크 재발급과 초대 참여를 워크스페이스 정책에 맞게 처리한다.
@Service
@RequiredArgsConstructor
public class WorkspaceInviteLinkCommandService {

    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final int INVITE_EXPIRES_HOURS = 24;

    private final SecureRandom secureRandom = new SecureRandom();
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMembershipRepository channelMembershipRepository;
    private final WorkspaceInviteLinkRepository workspaceInviteLinkRepository;

    @Transactional
    // 초대 권한이 있는 멤버의 명시적 요청에만 기존 링크를 폐기하고 새 토큰을 발급한다.
    public WorkspaceInviteLinkResponse issueInviteLink(UUID userId, UUID workspaceId) {
        WorkspaceMembership issuerMembership = validateInviteIssuer(userId, workspaceId);
        LocalDateTime now = LocalDateTime.now();
        workspaceInviteLinkRepository
                .findByWorkspaceIdAndStatus(workspaceId, WorkspaceInviteLinkStatus.ACTIVE)
                .ifPresent(activeInviteLink -> activeInviteLink.revoke(now));

        WorkspaceInviteLink inviteLink = WorkspaceInviteLink.create(
                workspaceId,
                issuerMembership.getId(),
                generateUniqueToken(),
                now.plusHours(INVITE_EXPIRES_HOURS)
        );
        workspaceInviteLinkRepository.save(inviteLink);

        return WorkspaceInviteLinkResponse.from(inviteLink);
    }

    @Transactional
    // 유효한 초대 토큰으로 참여하거나 기존 탈퇴 멤버십을 다시 활성화한다.
    public JoinedWorkspaceResponse joinWorkspace(UUID userId, String token) {
        User user = userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_UNAUTHORIZED));
        WorkspaceInviteLink inviteLink = workspaceInviteLinkRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(InviteErrorCode.INVITE_NOT_FOUND));
        validateUsable(inviteLink);

        Workspace workspace = workspaceRepository.findById(inviteLink.getWorkspaceId())
                .filter(foundWorkspace -> foundWorkspace.getStatus() != WorkspaceStatus.DELETED)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        if (workspace.getStatus() == WorkspaceStatus.ARCHIVED) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_ARCHIVED);
        }

        Channel generalChannel = channelRepository.findByWorkspaceIdAndGeneralTrue(workspace.getId())
                .orElseThrow(() -> new IllegalStateException("기본 채널이 없습니다."));
        WorkspaceMembership membership = joinOrReactivateMembership(workspace.getId(), user);
        joinGeneralChannel(generalChannel.getId(), membership.getId());

        return new JoinedWorkspaceResponse(workspace.getId(), generalChannel.getId());
    }

    private WorkspaceMembership validateInviteIssuer(UUID userId, UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .filter(foundWorkspace -> foundWorkspace.getStatus() != WorkspaceStatus.DELETED)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        if (workspace.getStatus() == WorkspaceStatus.ARCHIVED) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_ARCHIVED);
        }

        WorkspaceMembership membership = workspaceMembershipRepository
                .findByWorkspaceIdAndUserIdAndStatus(
                        workspaceId,
                        userId,
                        WorkspaceMembershipStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
        if (membership.getRole() == WorkspaceMembershipRole.MEMBER) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED);
        }
        return membership;
    }

    // 탈퇴 멤버는 기존 워크스페이스 프로필을 복원하고, 신규 사용자만 새 멤버십을 만든다.
    private WorkspaceMembership joinOrReactivateMembership(UUID workspaceId, User user) {
        return workspaceMembershipRepository.findByWorkspaceIdAndUserId(workspaceId, user.getId())
                .map(existingMembership -> {
                    if (existingMembership.getStatus() == WorkspaceMembershipStatus.REMOVED) {
                        throw new BusinessException(MemberErrorCode.MEMBER_REMOVED);
                    }
                    if (existingMembership.getStatus() == WorkspaceMembershipStatus.LEFT) {
                        existingMembership.rejoinAsMember(user);
                    }
                    return existingMembership;
                })
                .orElseGet(() -> workspaceMembershipRepository.save(
                        WorkspaceMembership.createMember(workspaceId, user)
                ));
    }

    // 초대 참여자는 기본 공개 채널에도 자동으로 접근할 수 있게 참여 상태를 맞춘다.
    private void joinGeneralChannel(UUID channelId, UUID membershipId) {
        channelMembershipRepository
                .findByChannelIdAndWorkspaceMembershipId(channelId, membershipId)
                .ifPresentOrElse(
                        ChannelMembership::rejoin,
                        () -> channelMembershipRepository.save(ChannelMembership.join(channelId, membershipId))
                );
    }

    private void validateUsable(WorkspaceInviteLink inviteLink) {
        if (inviteLink.getStatus() == WorkspaceInviteLinkStatus.REVOKED) {
            throw new BusinessException(InviteErrorCode.INVITE_REVOKED);
        }
        if (inviteLink.getStatus() == WorkspaceInviteLinkStatus.EXPIRED
                || inviteLink.isExpired(LocalDateTime.now())) {
            inviteLink.expire();
            throw new BusinessException(InviteErrorCode.INVITE_EXPIRED);
        }
    }

    // 토큰 충돌 시 재시도해 공개 URL이 기존 초대 링크를 가리키지 않게 한다.
    private String generateUniqueToken() {
        String token;
        do {
            byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
            secureRandom.nextBytes(tokenBytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        } while (workspaceInviteLinkRepository.existsByToken(token));
        return token;
    }
}
