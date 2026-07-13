package com.reecho.reechobe.channel.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reecho.reechobe.channel.domain.Channel;
import com.reecho.reechobe.channel.domain.ChannelMembership;
import com.reecho.reechobe.channel.domain.ChannelMembershipStatus;
import com.reecho.reechobe.channel.domain.ChannelVisibility;
import com.reecho.reechobe.channel.dto.AddChannelMembersRequest;
import com.reecho.reechobe.channel.dto.CreateChannelRequest;
import com.reecho.reechobe.channel.dto.CreatedChannelResponse;
import com.reecho.reechobe.channel.exception.ChannelErrorCode;
import com.reecho.reechobe.channel.repository.ChannelMembershipRepository;
import com.reecho.reechobe.channel.repository.ChannelRepository;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.common.exception.CommonErrorCode;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.exception.MemberErrorCode;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.user.domain.User;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChannelCommandServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMembershipRepository workspaceMembershipRepository;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private ChannelMembershipRepository channelMembershipRepository;

    private ChannelCommandService service;

    @BeforeEach
    void setUp() {
        service = new ChannelCommandService(
                workspaceRepository,
                workspaceMembershipRepository,
                channelRepository,
                channelMembershipRepository
        );
    }

    @Test
    void 오너는_채널을_생성하고_생성자를_참여자로_등록한다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership owner = createMembership(workspace.getId(), userId, WorkspaceMembershipRole.OWNER);
        CreateChannelRequest request = new CreateChannelRequest(
                " design ",
                " 디자인 논의 ",
                ChannelVisibility.PUBLIC,
                null
        );
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(owner));
        when(channelRepository.existsByWorkspaceIdAndName(workspace.getId(), "design")).thenReturn(false);
        when(workspaceMembershipRepository.findAllById(Set.of(owner.getId()))).thenReturn(List.of(owner));

        CreatedChannelResponse result = service.createChannel(userId, workspace.getId(), request);

        ArgumentCaptor<Channel> channelCaptor = ArgumentCaptor.forClass(Channel.class);
        ArgumentCaptor<ChannelMembership> channelMembershipCaptor = ArgumentCaptor.forClass(ChannelMembership.class);
        verify(channelRepository).save(channelCaptor.capture());
        verify(channelMembershipRepository).save(channelMembershipCaptor.capture());

        Channel savedChannel = channelCaptor.getValue();
        assertThat(result.id()).isEqualTo(savedChannel.getId());
        assertThat(savedChannel.getWorkspaceId()).isEqualTo(workspace.getId());
        assertThat(savedChannel.getName()).isEqualTo("design");
        assertThat(savedChannel.getDescription()).isEqualTo("디자인 논의");
        assertThat(savedChannel.getVisibility()).isEqualTo(ChannelVisibility.PUBLIC);
        assertThat(savedChannel.isGeneral()).isFalse();
        assertThat(savedChannel.getCreatedByMembershipId()).isEqualTo(owner.getId());
        assertThat(channelMembershipCaptor.getValue().getChannelId()).isEqualTo(savedChannel.getId());
        assertThat(channelMembershipCaptor.getValue().getWorkspaceMembershipId()).isEqualTo(owner.getId());
    }

    @Test
    void 관리자는_초기_멤버를_포함해_비공개_채널을_생성할_수_있다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership admin = createMembership(workspace.getId(), userId, WorkspaceMembershipRole.ADMIN);
        WorkspaceMembership member = createMembership(
                workspace.getId(),
                UUID.randomUUID(),
                WorkspaceMembershipRole.MEMBER
        );
        CreateChannelRequest request = new CreateChannelRequest(
                "private-room",
                null,
                ChannelVisibility.PRIVATE,
                Set.of(member.getId())
        );
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(admin));
        when(channelRepository.existsByWorkspaceIdAndName(workspace.getId(), "private-room")).thenReturn(false);
        when(workspaceMembershipRepository.findAllById(Set.of(admin.getId(), member.getId())))
                .thenReturn(List.of(admin, member));

        service.createChannel(userId, workspace.getId(), request);

        ArgumentCaptor<ChannelMembership> channelMembershipCaptor = ArgumentCaptor.forClass(ChannelMembership.class);
        verify(channelMembershipRepository, org.mockito.Mockito.times(2)).save(channelMembershipCaptor.capture());
        assertThat(channelMembershipCaptor.getAllValues())
                .extracting(ChannelMembership::getWorkspaceMembershipId)
                .containsExactly(admin.getId(), member.getId());
    }

    @Test
    void 일반_멤버는_채널을_생성할_수_없다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership member = createMembership(workspace.getId(), userId, WorkspaceMembershipRole.MEMBER);
        CreateChannelRequest request = new CreateChannelRequest(
                "design",
                null,
                ChannelVisibility.PUBLIC,
                null
        );
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> service.createChannel(userId, workspace.getId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ChannelErrorCode.CHANNEL_ACCESS_DENIED);

        verifyNoInteractions(channelRepository, channelMembershipRepository);
    }

    @Test
    void 관리자는_비공개_채널에_멤버를_추가할_수_있다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership admin = createMembership(workspace.getId(), userId, WorkspaceMembershipRole.ADMIN);
        WorkspaceMembership member = createMembership(
                workspace.getId(),
                UUID.randomUUID(),
                WorkspaceMembershipRole.MEMBER
        );
        Channel channel = Channel.create(
                workspace.getId(),
                "secret",
                null,
                ChannelVisibility.PRIVATE,
                admin.getId()
        );
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(admin));
        when(channelRepository.findById(channel.getId())).thenReturn(Optional.of(channel));
        when(workspaceMembershipRepository.findAllById(Set.of(member.getId()))).thenReturn(List.of(member));
        when(channelMembershipRepository.findByChannelIdAndWorkspaceMembershipId(channel.getId(), member.getId()))
                .thenReturn(Optional.empty());

        service.addPrivateChannelMembers(
                userId,
                workspace.getId(),
                channel.getId(),
                new AddChannelMembersRequest(Set.of(member.getId()))
        );

        ArgumentCaptor<ChannelMembership> channelMembershipCaptor =
                ArgumentCaptor.forClass(ChannelMembership.class);
        verify(channelMembershipRepository).save(channelMembershipCaptor.capture());
        assertThat(channelMembershipCaptor.getValue().getChannelId()).isEqualTo(channel.getId());
        assertThat(channelMembershipCaptor.getValue().getWorkspaceMembershipId()).isEqualTo(member.getId());
    }

    @Test
    void 나간_비공개_채널_멤버를_다시_추가하면_기존_멤버십을_복구한다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership owner = createMembership(workspace.getId(), userId, WorkspaceMembershipRole.OWNER);
        WorkspaceMembership member = createMembership(
                workspace.getId(),
                UUID.randomUUID(),
                WorkspaceMembershipRole.MEMBER
        );
        Channel channel = Channel.create(
                workspace.getId(),
                "secret",
                null,
                ChannelVisibility.PRIVATE,
                owner.getId()
        );
        ChannelMembership channelMembership = ChannelMembership.join(channel.getId(), member.getId());
        channelMembership.leave();
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(owner));
        when(channelRepository.findById(channel.getId())).thenReturn(Optional.of(channel));
        when(workspaceMembershipRepository.findAllById(Set.of(member.getId()))).thenReturn(List.of(member));
        when(channelMembershipRepository.findByChannelIdAndWorkspaceMembershipId(channel.getId(), member.getId()))
                .thenReturn(Optional.of(channelMembership));

        service.addPrivateChannelMembers(
                userId,
                workspace.getId(),
                channel.getId(),
                new AddChannelMembersRequest(Set.of(member.getId()))
        );

        assertThat(channelMembership.getStatus()).isEqualTo(ChannelMembershipStatus.ACTIVE);
        assertThat(channelMembership.getLeftAt()).isNull();
        verify(channelMembershipRepository, never()).save(any(ChannelMembership.class));
    }

    @Test
    void 일반_멤버는_비공개_채널에_멤버를_추가할_수_없다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership member = createMembership(workspace.getId(), userId, WorkspaceMembershipRole.MEMBER);
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> service.addPrivateChannelMembers(
                userId,
                workspace.getId(),
                UUID.randomUUID(),
                new AddChannelMembersRequest(Set.of(UUID.randomUUID()))
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ChannelErrorCode.CHANNEL_ACCESS_DENIED);

        verifyNoInteractions(channelRepository, channelMembershipRepository);
    }

    @Test
    void 공개_채널에는_비공개_채널_멤버_추가_api를_사용할_수_없다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership owner = createMembership(workspace.getId(), userId, WorkspaceMembershipRole.OWNER);
        WorkspaceMembership member = createMembership(
                workspace.getId(),
                UUID.randomUUID(),
                WorkspaceMembershipRole.MEMBER
        );
        Channel channel = Channel.create(workspace.getId(), "design", null, ChannelVisibility.PUBLIC, owner.getId());
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(owner));
        when(channelRepository.findById(channel.getId())).thenReturn(Optional.of(channel));

        assertThatThrownBy(() -> service.addPrivateChannelMembers(
                userId,
                workspace.getId(),
                channel.getId(),
                new AddChannelMembersRequest(Set.of(member.getId()))
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.VALIDATION_ERROR);

        verify(channelMembershipRepository, never()).save(any(ChannelMembership.class));
    }

    @Test
    void 다른_워크스페이스_멤버는_비공개_채널에_추가할_수_없다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership owner = createMembership(workspace.getId(), userId, WorkspaceMembershipRole.OWNER);
        WorkspaceMembership otherWorkspaceMember = createMembership(
                UUID.randomUUID(),
                UUID.randomUUID(),
                WorkspaceMembershipRole.MEMBER
        );
        Channel channel = Channel.create(
                workspace.getId(),
                "secret",
                null,
                ChannelVisibility.PRIVATE,
                owner.getId()
        );
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(owner));
        when(channelRepository.findById(channel.getId())).thenReturn(Optional.of(channel));
        when(workspaceMembershipRepository.findAllById(Set.of(otherWorkspaceMember.getId())))
                .thenReturn(List.of(otherWorkspaceMember));

        assertThatThrownBy(() -> service.addPrivateChannelMembers(
                userId,
                workspace.getId(),
                channel.getId(),
                new AddChannelMembersRequest(Set.of(otherWorkspaceMember.getId()))
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);

        verify(channelMembershipRepository, never()).save(any(ChannelMembership.class));
    }

    @Test
    void 보관된_워크스페이스에는_채널을_생성할_수_없다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        ReflectionTestUtils.setField(workspace, "status", WorkspaceStatus.ARCHIVED);
        CreateChannelRequest request = new CreateChannelRequest(
                "design",
                null,
                ChannelVisibility.PUBLIC,
                null
        );
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));

        assertThatThrownBy(() -> service.createChannel(userId, workspace.getId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_ARCHIVED);

        verifyNoInteractions(workspaceMembershipRepository, channelRepository, channelMembershipRepository);
    }

    @Test
    void 같은_워크스페이스에_이미_있는_채널명은_사용할_수_없다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership owner = createMembership(workspace.getId(), userId, WorkspaceMembershipRole.OWNER);
        CreateChannelRequest request = new CreateChannelRequest(
                "design",
                null,
                ChannelVisibility.PUBLIC,
                null
        );
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(owner));
        when(channelRepository.existsByWorkspaceIdAndName(workspace.getId(), "design")).thenReturn(true);

        assertThatThrownBy(() -> service.createChannel(userId, workspace.getId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.VALIDATION_ERROR);

        verify(channelRepository, never()).save(any(Channel.class));
        verifyNoInteractions(channelMembershipRepository);
    }

    @Test
    void 초기_멤버가_같은_워크스페이스의_활성_멤버가_아니면_생성할_수_없다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership owner = createMembership(workspace.getId(), userId, WorkspaceMembershipRole.OWNER);
        UUID invalidMemberId = UUID.randomUUID();
        CreateChannelRequest request = new CreateChannelRequest(
                "private-room",
                null,
                ChannelVisibility.PRIVATE,
                Set.of(invalidMemberId)
        );
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(owner));
        when(channelRepository.existsByWorkspaceIdAndName(workspace.getId(), "private-room")).thenReturn(false);
        when(workspaceMembershipRepository.findAllById(Set.of(owner.getId(), invalidMemberId)))
                .thenReturn(List.of(owner));

        assertThatThrownBy(() -> service.createChannel(userId, workspace.getId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);

        verify(channelRepository, never()).save(any(Channel.class));
        verifyNoInteractions(channelMembershipRepository);
    }

    @Test
    void 공개_채널에_처음_참여하면_채널_멤버십을_생성한다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership member = createMembership(workspace.getId(), userId, WorkspaceMembershipRole.MEMBER);
        Channel channel = Channel.create(workspace.getId(), "design", null, ChannelVisibility.PUBLIC, member.getId());
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(member));
        when(channelRepository.findById(channel.getId())).thenReturn(Optional.of(channel));
        when(channelMembershipRepository.findByChannelIdAndWorkspaceMembershipId(channel.getId(), member.getId()))
                .thenReturn(Optional.empty());

        service.joinPublicChannel(userId, workspace.getId(), channel.getId());

        ArgumentCaptor<ChannelMembership> channelMembershipCaptor =
                ArgumentCaptor.forClass(ChannelMembership.class);
        verify(channelMembershipRepository).save(channelMembershipCaptor.capture());
        assertThat(channelMembershipCaptor.getValue().getChannelId()).isEqualTo(channel.getId());
        assertThat(channelMembershipCaptor.getValue().getWorkspaceMembershipId()).isEqualTo(member.getId());
        assertThat(channelMembershipCaptor.getValue().getStatus()).isEqualTo(ChannelMembershipStatus.ACTIVE);
    }

    @Test
    void 나간_공개_채널에_재참여하면_기존_멤버십을_복구한다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership member = createMembership(workspace.getId(), userId, WorkspaceMembershipRole.MEMBER);
        Channel channel = Channel.create(workspace.getId(), "design", null, ChannelVisibility.PUBLIC, member.getId());
        ChannelMembership channelMembership = ChannelMembership.join(channel.getId(), member.getId());
        channelMembership.leave();
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(member));
        when(channelRepository.findById(channel.getId())).thenReturn(Optional.of(channel));
        when(channelMembershipRepository.findByChannelIdAndWorkspaceMembershipId(channel.getId(), member.getId()))
                .thenReturn(Optional.of(channelMembership));

        service.joinPublicChannel(userId, workspace.getId(), channel.getId());

        assertThat(channelMembership.getStatus()).isEqualTo(ChannelMembershipStatus.ACTIVE);
        assertThat(channelMembership.getLeftAt()).isNull();
        verify(channelMembershipRepository, never()).save(any(ChannelMembership.class));
    }

    @Test
    void 이미_참여_중인_공개_채널에는_다시_참여할_수_없다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership member = createMembership(workspace.getId(), userId, WorkspaceMembershipRole.MEMBER);
        Channel channel = Channel.create(workspace.getId(), "design", null, ChannelVisibility.PUBLIC, member.getId());
        ChannelMembership channelMembership = ChannelMembership.join(channel.getId(), member.getId());
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(member));
        when(channelRepository.findById(channel.getId())).thenReturn(Optional.of(channel));
        when(channelMembershipRepository.findByChannelIdAndWorkspaceMembershipId(channel.getId(), member.getId()))
                .thenReturn(Optional.of(channelMembership));

        assertThatThrownBy(() -> service.joinPublicChannel(userId, workspace.getId(), channel.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ChannelErrorCode.CHANNEL_ALREADY_JOINED);

        verify(channelMembershipRepository, never()).save(any(ChannelMembership.class));
    }

    @Test
    void 비공개_채널에는_공개_채널_참여_api로_참여할_수_없다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership member = createMembership(workspace.getId(), userId, WorkspaceMembershipRole.MEMBER);
        Channel channel = Channel.create(
                workspace.getId(),
                "secret",
                null,
                ChannelVisibility.PRIVATE,
                member.getId()
        );
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(member));
        when(channelRepository.findById(channel.getId())).thenReturn(Optional.of(channel));

        assertThatThrownBy(() -> service.joinPublicChannel(userId, workspace.getId(), channel.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ChannelErrorCode.CHANNEL_JOIN_FORBIDDEN);

        verifyNoInteractions(channelMembershipRepository);
    }

    @Test
    void 참여_중인_공개_채널에서_나갈_수_있다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership member = createMembership(workspace.getId(), userId, WorkspaceMembershipRole.MEMBER);
        Channel channel = Channel.create(workspace.getId(), "design", null, ChannelVisibility.PUBLIC, member.getId());
        ChannelMembership channelMembership = ChannelMembership.join(channel.getId(), member.getId());
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(member));
        when(channelRepository.findById(channel.getId())).thenReturn(Optional.of(channel));
        when(channelMembershipRepository.findByChannelIdAndWorkspaceMembershipId(channel.getId(), member.getId()))
                .thenReturn(Optional.of(channelMembership));

        service.leavePublicChannel(userId, workspace.getId(), channel.getId());

        assertThat(channelMembership.getStatus()).isEqualTo(ChannelMembershipStatus.LEFT);
        assertThat(channelMembership.getLeftAt()).isNotNull();
    }

    @Test
    void 기본_채널에서는_나갈_수_없다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership member = createMembership(workspace.getId(), userId, WorkspaceMembershipRole.MEMBER);
        Channel channel = Channel.createGeneral(workspace.getId(), member.getId());
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(member));
        when(channelRepository.findById(channel.getId())).thenReturn(Optional.of(channel));

        assertThatThrownBy(() -> service.leavePublicChannel(userId, workspace.getId(), channel.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ChannelErrorCode.CHANNEL_GENERAL_LEAVE_FORBIDDEN);

        verifyNoInteractions(channelMembershipRepository);
    }

    private Workspace createWorkspace(UUID createdByUserId) {
        return Workspace.create("Re-Echo Team", null, null, createdByUserId);
    }

    private WorkspaceMembership createMembership(
            UUID workspaceId,
            UUID userId,
            WorkspaceMembershipRole role
    ) {
        User user = User.createActive("사용자", null);
        ReflectionTestUtils.setField(user, "id", userId);
        WorkspaceMembership membership = role == WorkspaceMembershipRole.OWNER
                ? WorkspaceMembership.createOwner(workspaceId, user)
                : WorkspaceMembership.createMember(workspaceId, user);
        ReflectionTestUtils.setField(membership, "role", role);
        return membership;
    }
}
