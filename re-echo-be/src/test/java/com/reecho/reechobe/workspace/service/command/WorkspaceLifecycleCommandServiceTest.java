package com.reecho.reechobe.workspace.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reecho.reechobe.channel.domain.Channel;
import com.reecho.reechobe.channel.domain.ChannelStatus;
import com.reecho.reechobe.channel.repository.ChannelRepository;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.user.domain.User;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WorkspaceLifecycleCommandServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMembershipRepository workspaceMembershipRepository;

    @Mock
    private ChannelRepository channelRepository;

    private WorkspaceLifecycleCommandService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceLifecycleCommandService(
                workspaceRepository,
                workspaceMembershipRepository,
                channelRepository
        );
    }

    @Test
    void 소유자는_워크스페이스와_활성_채널을_같은_만료_시각으로_보관할_수_있다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership ownerMembership = createOwnerMembership(workspace.getId(), userId);
        Channel generalChannel = Channel.createGeneral(workspace.getId(), ownerMembership.getId());
        Channel projectChannel = Channel.create(
                workspace.getId(), "project", null, null, ownerMembership.getId()
        );
        prepareOwner(workspace, ownerMembership, userId);
        when(channelRepository.findByWorkspaceIdAndStatus(workspace.getId(), ChannelStatus.ACTIVE))
                .thenReturn(List.of(generalChannel, projectChannel));

        service.archiveWorkspace(userId, workspace.getId());

        assertThat(workspace.getStatus()).isEqualTo(WorkspaceStatus.ARCHIVED);
        assertThat(workspace.getArchivedAt()).isNotNull();
        assertThat(workspace.getArchiveExpiresAt())
                .isEqualTo(workspace.getArchivedAt().plusDays(15));
        assertThat(generalChannel.getStatus()).isEqualTo(ChannelStatus.ARCHIVED);
        assertThat(projectChannel.getArchivedAt()).isEqualTo(workspace.getArchivedAt());
        assertThat(projectChannel.getArchiveExpiresAt()).isEqualTo(workspace.getArchiveExpiresAt());
    }

    @Test
    void 일반_멤버는_워크스페이스를_보관할_수_없다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(UUID.randomUUID());
        WorkspaceMembership memberMembership = createMemberMembership(workspace.getId(), userId);
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(), userId, WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(memberMembership));

        assertThatThrownBy(() -> service.archiveWorkspace(userId, workspace.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED);

        verifyNoInteractions(channelRepository);
    }

    @Test
    void 만료_전_소유자는_워크스페이스_보관으로_전환된_채널만_복원할_수_있다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership ownerMembership = createOwnerMembership(workspace.getId(), userId);
        LocalDateTime archivedAt = LocalDateTime.now().minusDays(1);
        workspace.archive(archivedAt, archivedAt.plusDays(15));
        Channel generalChannel = Channel.createGeneral(workspace.getId(), ownerMembership.getId());
        generalChannel.archive(archivedAt, archivedAt.plusDays(15));
        prepareOwner(workspace, ownerMembership, userId);
        when(channelRepository.findByWorkspaceIdAndStatusAndArchivedAt(
                workspace.getId(), ChannelStatus.ARCHIVED, archivedAt
        )).thenReturn(List.of(generalChannel));

        service.restoreWorkspace(userId, workspace.getId());

        assertThat(workspace.getStatus()).isEqualTo(WorkspaceStatus.ACTIVE);
        assertThat(workspace.getArchivedAt()).isNull();
        assertThat(generalChannel.getStatus()).isEqualTo(ChannelStatus.ACTIVE);
        assertThat(generalChannel.getArchivedAt()).isNull();
    }

    @Test
    void 만료된_워크스페이스는_복원할_수_없다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership ownerMembership = createOwnerMembership(workspace.getId(), userId);
        LocalDateTime archivedAt = LocalDateTime.now().minusDays(16);
        workspace.archive(archivedAt, archivedAt.plusDays(15));
        prepareOwner(workspace, ownerMembership, userId);

        assertThatThrownBy(() -> service.restoreWorkspace(userId, workspace.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_RESTORE_NOT_ALLOWED);

        verifyNoInteractions(channelRepository);
    }

    private void prepareOwner(Workspace workspace, WorkspaceMembership membership, UUID userId) {
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(), userId, WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(membership));
    }

    private Workspace createWorkspace(UUID createdByUserId) {
        return Workspace.create("Re-Echo Team", null, null, createdByUserId);
    }

    private WorkspaceMembership createOwnerMembership(UUID workspaceId, UUID userId) {
        User user = User.createActive("소유자", null);
        ReflectionTestUtils.setField(user, "id", userId);
        return WorkspaceMembership.createOwner(workspaceId, user);
    }

    private WorkspaceMembership createMemberMembership(UUID workspaceId, UUID userId) {
        User user = User.createActive("멤버", null);
        ReflectionTestUtils.setField(user, "id", userId);
        return WorkspaceMembership.createMember(workspaceId, user);
    }
}
