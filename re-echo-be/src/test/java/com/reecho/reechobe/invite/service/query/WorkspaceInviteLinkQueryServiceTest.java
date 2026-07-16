package com.reecho.reechobe.invite.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.invite.domain.WorkspaceInviteLink;
import com.reecho.reechobe.invite.domain.WorkspaceInviteLinkStatus;
import com.reecho.reechobe.invite.exception.InviteErrorCode;
import com.reecho.reechobe.invite.repository.WorkspaceInviteLinkRepository;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.user.domain.User;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WorkspaceInviteLinkQueryServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMembershipRepository workspaceMembershipRepository;

    @Mock
    private WorkspaceInviteLinkRepository workspaceInviteLinkRepository;

    private WorkspaceInviteLinkQueryService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceInviteLinkQueryService(
                workspaceRepository,
                workspaceMembershipRepository,
                workspaceInviteLinkRepository
        );
    }

    @Test
    void 관리자는_활성_초대_링크를_조회한다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(UUID.randomUUID());
        WorkspaceMembership adminMembership = createMembership(workspace.getId(), userId);
        ReflectionTestUtils.setField(adminMembership, "role", WorkspaceMembershipRole.ADMIN);
        WorkspaceInviteLink inviteLink = WorkspaceInviteLink.create(
                workspace.getId(),
                adminMembership.getId(),
                "invite-token",
                LocalDateTime.now().plusHours(1)
        );
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(adminMembership));
        when(workspaceInviteLinkRepository.findByWorkspaceIdAndStatus(
                workspace.getId(),
                WorkspaceInviteLinkStatus.ACTIVE
        )).thenReturn(Optional.of(inviteLink));

        var result = service.getActiveInviteLink(userId, workspace.getId());

        assertThat(result.token()).isEqualTo("invite-token");
        assertThat(result.expiresAt()).isEqualTo(inviteLink.getExpiresAt());
    }

    @Test
    void 초대_링크_미리보기는_로그인_없이_워크스페이스_정보를_반환한다() {
        Workspace workspace = createWorkspace(UUID.randomUUID());
        WorkspaceInviteLink inviteLink = WorkspaceInviteLink.create(
                workspace.getId(),
                UUID.randomUUID(),
                "invite-token",
                LocalDateTime.now().plusHours(1)
        );
        when(workspaceInviteLinkRepository.findByToken("invite-token")).thenReturn(Optional.of(inviteLink));
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));

        var result = service.previewInviteLink("invite-token");

        assertThat(result.workspaceName()).isEqualTo("Re-Echo Team");
        assertThat(result.workspaceImageUrl()).isEqualTo("https://example.com/workspace.png");
        assertThat(result.expiresAt()).isEqualTo(inviteLink.getExpiresAt());
    }

    @Test
    void 만료된_초대_링크는_상태를_만료로_바꾸고_예외를_던진다() {
        WorkspaceInviteLink inviteLink = WorkspaceInviteLink.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "invite-token",
                LocalDateTime.now().minusMinutes(1)
        );
        when(workspaceInviteLinkRepository.findByToken("invite-token")).thenReturn(Optional.of(inviteLink));

        assertThatThrownBy(() -> service.previewInviteLink("invite-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(InviteErrorCode.INVITE_EXPIRED);

        assertThat(inviteLink.getStatus()).isEqualTo(WorkspaceInviteLinkStatus.EXPIRED);
    }

    private Workspace createWorkspace(UUID createdByUserId) {
        return Workspace.create(
                "Re-Echo Team",
                null,
                "https://example.com/workspace.png",
                createdByUserId
        );
    }

    private WorkspaceMembership createMembership(UUID workspaceId, UUID userId) {
        User user = User.createActive("관리자", null);
        ReflectionTestUtils.setField(user, "id", userId);
        WorkspaceMembership membership = WorkspaceMembership.createMember(workspaceId, user);
        ReflectionTestUtils.setField(membership, "id", UUID.randomUUID());
        return membership;
    }
}
