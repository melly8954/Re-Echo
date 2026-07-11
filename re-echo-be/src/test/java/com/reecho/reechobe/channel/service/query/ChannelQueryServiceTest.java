package com.reecho.reechobe.channel.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reecho.reechobe.channel.domain.Channel;
import com.reecho.reechobe.channel.domain.ChannelMembershipStatus;
import com.reecho.reechobe.channel.dto.ChannelListResponse;
import com.reecho.reechobe.channel.repository.ChannelMembershipRepository;
import com.reecho.reechobe.channel.repository.ChannelRepository;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.user.domain.User;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
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
class ChannelQueryServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMembershipRepository workspaceMembershipRepository;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private ChannelMembershipRepository channelMembershipRepository;

    private ChannelQueryService service;

    @BeforeEach
    void setUp() {
        service = new ChannelQueryService(
                workspaceRepository,
                workspaceMembershipRepository,
                channelRepository,
                channelMembershipRepository
        );
    }

    @Test
    void 활성_멤버는_접근_가능한_채널과_참여_상태를_조회한다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership membership = createMembership(workspace.getId(), userId);
        Channel generalChannel = Channel.createGeneral(workspace.getId(), membership.getId());
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(membership));
        when(channelMembershipRepository.findChannelIdsByWorkspaceMembershipIdAndStatus(
                membership.getId(),
                ChannelMembershipStatus.ACTIVE
        )).thenReturn(List.of(generalChannel.getId()));
        when(channelRepository.findAccessibleActiveChannels(workspace.getId(), membership.getId()))
                .thenReturn(List.of(generalChannel));

        ChannelListResponse result = service.getChannels(userId, workspace.getId());

        assertThat(result.contents()).singleElement().satisfies(channel -> {
            assertThat(channel.id()).isEqualTo(generalChannel.getId());
            assertThat(channel.name()).isEqualTo("general");
            assertThat(channel.general()).isTrue();
            assertThat(channel.joined()).isTrue();
            assertThat(channel.unreadCount()).isZero();
        });
    }

    @Test
    void 워크스페이스가_없으면_채널을_조회할_수_없다() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getChannels(userId, workspaceId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);

        verifyNoInteractions(
                workspaceMembershipRepository,
                channelRepository,
                channelMembershipRepository
        );
    }

    @Test
    void 활성_멤버가_아니면_채널을_조회할_수_없다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(UUID.randomUUID());
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getChannels(userId, workspace.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED);

        verifyNoInteractions(channelRepository, channelMembershipRepository);
    }

    private Workspace createWorkspace(UUID createdByUserId) {
        return Workspace.create("Re-Echo Team", null, null, createdByUserId);
    }

    private WorkspaceMembership createMembership(UUID workspaceId, UUID userId) {
        User user = User.createActive("사용자", null);
        ReflectionTestUtils.setField(user, "id", userId);
        return WorkspaceMembership.createOwner(workspaceId, user);
    }
}
