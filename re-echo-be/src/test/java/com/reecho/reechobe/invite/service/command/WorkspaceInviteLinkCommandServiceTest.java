package com.reecho.reechobe.invite.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reecho.reechobe.channel.domain.Channel;
import com.reecho.reechobe.channel.domain.ChannelMembership;
import com.reecho.reechobe.channel.repository.ChannelMembershipRepository;
import com.reecho.reechobe.channel.repository.ChannelRepository;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.invite.domain.WorkspaceInviteLink;
import com.reecho.reechobe.invite.domain.WorkspaceInviteLinkStatus;
import com.reecho.reechobe.invite.repository.WorkspaceInviteLinkRepository;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.exception.MemberErrorCode;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.user.domain.User;
import com.reecho.reechobe.user.repository.UserRepository;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.dto.CreatedWorkspaceResponse;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WorkspaceInviteLinkCommandServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMembershipRepository workspaceMembershipRepository;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private ChannelMembershipRepository channelMembershipRepository;

    @Mock
    private WorkspaceInviteLinkRepository workspaceInviteLinkRepository;

    private WorkspaceInviteLinkCommandService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceInviteLinkCommandService(
                userRepository,
                workspaceRepository,
                workspaceMembershipRepository,
                channelRepository,
                channelMembershipRepository,
                workspaceInviteLinkRepository
        );
    }

    @Test
    void 초대_링크를_재발급하면_기존_활성_링크를_무효화한다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership adminMembership = createMembership(workspace.getId(), userId);
        ReflectionTestUtils.setField(adminMembership, "role", WorkspaceMembershipRole.ADMIN);
        WorkspaceInviteLink oldInviteLink = WorkspaceInviteLink.create(
                workspace.getId(),
                adminMembership.getId(),
                "old-token",
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
        )).thenReturn(Optional.of(oldInviteLink));
        when(workspaceInviteLinkRepository.existsByToken(anyString())).thenReturn(false);

        var result = service.issueInviteLink(userId, workspace.getId());

        ArgumentCaptor<WorkspaceInviteLink> inviteCaptor =
                ArgumentCaptor.forClass(WorkspaceInviteLink.class);
        verify(workspaceInviteLinkRepository).save(inviteCaptor.capture());
        WorkspaceInviteLink newInviteLink = inviteCaptor.getValue();

        assertThat(oldInviteLink.getStatus()).isEqualTo(WorkspaceInviteLinkStatus.REVOKED);
        assertThat(oldInviteLink.getRevokedAt()).isNotNull();
        assertThat(newInviteLink.getWorkspaceId()).isEqualTo(workspace.getId());
        assertThat(newInviteLink.getCreatedByMembershipId()).isEqualTo(adminMembership.getId());
        assertThat(newInviteLink.getStatus()).isEqualTo(WorkspaceInviteLinkStatus.ACTIVE);
        assertThat(newInviteLink.getToken()).isNotBlank();
        assertThat(result.token()).isEqualTo(newInviteLink.getToken());
    }

    @Test
    void 일반_멤버는_초대_링크를_발급할_수_없다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(UUID.randomUUID());
        WorkspaceMembership memberMembership = createMembership(workspace.getId(), userId);
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(memberMembership));

        assertThatThrownBy(() -> service.issueInviteLink(userId, workspace.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED);

        verifyNoInteractions(workspaceInviteLinkRepository);
    }

    @Test
    void 초대_링크로_새_멤버십을_만들고_general_채널에_참여시킨다() {
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        Workspace workspace = createWorkspace(UUID.randomUUID());
        WorkspaceMembership issuerMembership = createMembership(workspace.getId(), UUID.randomUUID());
        WorkspaceInviteLink inviteLink = WorkspaceInviteLink.create(
                workspace.getId(),
                issuerMembership.getId(),
                "invite-token",
                LocalDateTime.now().plusHours(1)
        );
        Channel generalChannel = Channel.createGeneral(workspace.getId(), issuerMembership.getId());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(workspaceInviteLinkRepository.findByToken("invite-token")).thenReturn(Optional.of(inviteLink));
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(channelRepository.findByWorkspaceIdAndGeneralTrue(workspace.getId()))
                .thenReturn(Optional.of(generalChannel));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserId(workspace.getId(), userId))
                .thenReturn(Optional.empty());
        when(workspaceMembershipRepository.save(any(WorkspaceMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(channelMembershipRepository.findByChannelIdAndWorkspaceMembershipId(
                any(UUID.class),
                any(UUID.class)
        )).thenReturn(Optional.empty());

        var result = service.joinWorkspace(userId, "invite-token");

        ArgumentCaptor<WorkspaceMembership> membershipCaptor =
                ArgumentCaptor.forClass(WorkspaceMembership.class);
        ArgumentCaptor<ChannelMembership> channelMembershipCaptor =
                ArgumentCaptor.forClass(ChannelMembership.class);
        verify(workspaceMembershipRepository).save(membershipCaptor.capture());
        verify(channelMembershipRepository).save(channelMembershipCaptor.capture());
        WorkspaceMembership membership = membershipCaptor.getValue();
        ChannelMembership channelMembership = channelMembershipCaptor.getValue();

        assertThat(result.id()).isEqualTo(workspace.getId());
        assertThat(result.defaultChannelId()).isEqualTo(generalChannel.getId());
        assertThat(membership.getWorkspaceId()).isEqualTo(workspace.getId());
        assertThat(membership.getUserId()).isEqualTo(userId);
        assertThat(membership.getRole()).isEqualTo(WorkspaceMembershipRole.MEMBER);
        assertThat(membership.getDisplayName()).isEqualTo("초대 사용자");
        assertThat(membership.getStatus()).isEqualTo(WorkspaceMembershipStatus.ACTIVE);
        assertThat(channelMembership.getChannelId()).isEqualTo(generalChannel.getId());
        assertThat(channelMembership.getWorkspaceMembershipId()).isEqualTo(membership.getId());
    }

    @Test
    void 강제_제거된_멤버는_초대_링크로_재참여할_수_없다() {
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        Workspace workspace = createWorkspace(UUID.randomUUID());
        WorkspaceMembership removedMembership = createMembership(workspace.getId(), userId);
        ReflectionTestUtils.setField(removedMembership, "status", WorkspaceMembershipStatus.REMOVED);
        WorkspaceInviteLink inviteLink = WorkspaceInviteLink.create(
                workspace.getId(),
                UUID.randomUUID(),
                "invite-token",
                LocalDateTime.now().plusHours(1)
        );
        Channel generalChannel = Channel.createGeneral(workspace.getId(), UUID.randomUUID());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(workspaceInviteLinkRepository.findByToken("invite-token")).thenReturn(Optional.of(inviteLink));
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(channelRepository.findByWorkspaceIdAndGeneralTrue(workspace.getId()))
                .thenReturn(Optional.of(generalChannel));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserId(workspace.getId(), userId))
                .thenReturn(Optional.of(removedMembership));

        assertThatThrownBy(() -> service.joinWorkspace(userId, "invite-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MemberErrorCode.MEMBER_REMOVED);

        verifyNoInteractions(channelMembershipRepository);
    }

    private Workspace createWorkspace(UUID createdByUserId) {
        return Workspace.create("Re-Echo Team", null, null, createdByUserId);
    }

    private WorkspaceMembership createMembership(UUID workspaceId, UUID userId) {
        User user = createUser(userId);
        WorkspaceMembership membership = WorkspaceMembership.createMember(workspaceId, user);
        ReflectionTestUtils.setField(membership, "id", UUID.randomUUID());
        return membership;
    }

    private User createUser(UUID userId) {
        User user = User.createActive("초대 사용자", null);
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
