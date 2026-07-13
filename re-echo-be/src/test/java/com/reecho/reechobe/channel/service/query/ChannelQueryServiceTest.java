package com.reecho.reechobe.channel.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reecho.reechobe.channel.domain.Channel;
import com.reecho.reechobe.channel.domain.ChannelMembership;
import com.reecho.reechobe.channel.domain.ChannelMembershipStatus;
import com.reecho.reechobe.channel.domain.ChannelVisibility;
import com.reecho.reechobe.channel.dto.ChannelListResponse;
import com.reecho.reechobe.channel.exception.ChannelErrorCode;
import com.reecho.reechobe.channel.repository.ChannelMemberCountProjection;
import com.reecho.reechobe.channel.repository.ChannelMembershipRepository;
import com.reecho.reechobe.channel.repository.ChannelRepository;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
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
        ChannelMemberCountProjection memberCount = mock(ChannelMemberCountProjection.class);
        when(memberCount.getChannelId()).thenReturn(generalChannel.getId());
        when(memberCount.getMemberCount()).thenReturn(2L);
        when(channelMembershipRepository.countByChannelIdsAndStatus(
                List.of(generalChannel.getId()),
                ChannelMembershipStatus.ACTIVE
        )).thenReturn(List.of(memberCount));

        ChannelListResponse result = service.getChannels(userId, workspace.getId());

        assertThat(result.contents()).singleElement().satisfies(channel -> {
            assertThat(channel.id()).isEqualTo(generalChannel.getId());
            assertThat(channel.name()).isEqualTo("general");
            assertThat(channel.general()).isTrue();
            assertThat(channel.joined()).isTrue();
            assertThat(channel.unreadCount()).isZero();
            assertThat(channel.memberCount()).isEqualTo(2L);
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

    @Test
    void 공개_채널은_워크스페이스_멤버가_채널_멤버_목록을_조회할_수_있다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(UUID.randomUUID());
        WorkspaceMembership requester = createMembership(
                workspace.getId(),
                userId,
                "요청자",
                WorkspaceMembershipRole.MEMBER
        );
        WorkspaceMembership owner = createMembership(
                workspace.getId(),
                UUID.randomUUID(),
                "소유자",
                WorkspaceMembershipRole.OWNER
        );
        WorkspaceMembership member = createMembership(
                workspace.getId(),
                UUID.randomUUID(),
                "멤버",
                WorkspaceMembershipRole.MEMBER
        );
        Channel channel = Channel.create(workspace.getId(), "design", null, ChannelVisibility.PUBLIC, owner.getId());
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(requester));
        when(channelRepository.findById(channel.getId())).thenReturn(Optional.of(channel));
        when(workspaceMembershipRepository.findActiveChannelMembers(
                channel.getId(),
                WorkspaceMembershipStatus.ACTIVE,
                ChannelMembershipStatus.ACTIVE
        )).thenReturn(List.of(member, owner));

        var result = service.getChannelMembers(userId, workspace.getId(), channel.getId());

        assertThat(result.contents()).extracting("displayName")
                .containsExactly("소유자", "멤버");
        assertThat(result.contents()).extracting("role")
                .containsExactly(WorkspaceMembershipRole.OWNER, WorkspaceMembershipRole.MEMBER);
    }

    @Test
    void 비공개_채널은_참여자만_채널_멤버_목록을_조회할_수_있다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(UUID.randomUUID());
        WorkspaceMembership requester = createMembership(
                workspace.getId(),
                userId,
                "요청자",
                WorkspaceMembershipRole.MEMBER
        );
        WorkspaceMembership admin = createMembership(
                workspace.getId(),
                UUID.randomUUID(),
                "관리자",
                WorkspaceMembershipRole.ADMIN
        );
        Channel channel = Channel.create(workspace.getId(), "secret", null, ChannelVisibility.PRIVATE, admin.getId());
        ChannelMembership channelMembership = ChannelMembership.join(channel.getId(), requester.getId());
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(requester));
        when(channelRepository.findById(channel.getId())).thenReturn(Optional.of(channel));
        when(channelMembershipRepository.findByChannelIdAndWorkspaceMembershipId(
                channel.getId(),
                requester.getId()
        )).thenReturn(Optional.of(channelMembership));
        when(workspaceMembershipRepository.findActiveChannelMembers(
                channel.getId(),
                WorkspaceMembershipStatus.ACTIVE,
                ChannelMembershipStatus.ACTIVE
        )).thenReturn(List.of(requester, admin));

        var result = service.getChannelMembers(userId, workspace.getId(), channel.getId());

        assertThat(result.contents()).extracting("displayName")
                .containsExactly("관리자", "요청자");
    }

    @Test
    void 비공개_채널_비참여자는_채널_멤버_목록을_조회할_수_없다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(UUID.randomUUID());
        WorkspaceMembership requester = createMembership(
                workspace.getId(),
                userId,
                "요청자",
                WorkspaceMembershipRole.MEMBER
        );
        WorkspaceMembership admin = createMembership(
                workspace.getId(),
                UUID.randomUUID(),
                "관리자",
                WorkspaceMembershipRole.ADMIN
        );
        Channel channel = Channel.create(workspace.getId(), "secret", null, ChannelVisibility.PRIVATE, admin.getId());
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(requester));
        when(channelRepository.findById(channel.getId())).thenReturn(Optional.of(channel));
        when(channelMembershipRepository.findByChannelIdAndWorkspaceMembershipId(
                channel.getId(),
                requester.getId()
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getChannelMembers(userId, workspace.getId(), channel.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ChannelErrorCode.CHANNEL_ACCESS_DENIED);
    }

    @Test
    void 다른_워크스페이스_채널의_멤버_목록은_조회할_수_없다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(UUID.randomUUID());
        UUID otherWorkspaceId = UUID.randomUUID();
        WorkspaceMembership requester = createMembership(
                workspace.getId(),
                userId,
                "요청자",
                WorkspaceMembershipRole.MEMBER
        );
        Channel channel = Channel.create(otherWorkspaceId, "design", null, ChannelVisibility.PUBLIC, requester.getId());
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(requester));
        when(channelRepository.findById(channel.getId())).thenReturn(Optional.of(channel));

        assertThatThrownBy(() -> service.getChannelMembers(userId, workspace.getId(), channel.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ChannelErrorCode.CHANNEL_NOT_FOUND);
    }

    private Workspace createWorkspace(UUID createdByUserId) {
        return Workspace.create("Re-Echo Team", null, null, createdByUserId);
    }

    private WorkspaceMembership createMembership(UUID workspaceId, UUID userId) {
        return createMembership(workspaceId, userId, "사용자", WorkspaceMembershipRole.OWNER);
    }

    private WorkspaceMembership createMembership(
            UUID workspaceId,
            UUID userId,
            String displayName,
            WorkspaceMembershipRole role
    ) {
        User user = User.createActive(displayName, null);
        ReflectionTestUtils.setField(user, "id", userId);
        WorkspaceMembership membership = role == WorkspaceMembershipRole.OWNER
                ? WorkspaceMembership.createOwner(workspaceId, user)
                : WorkspaceMembership.createMember(workspaceId, user);
        ReflectionTestUtils.setField(membership, "role", role);
        return membership;
    }
}
