package com.reecho.reechobe.workspace.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.channel.domain.Channel;
import com.reecho.reechobe.channel.domain.ChannelMembership;
import com.reecho.reechobe.channel.domain.ChannelMembershipStatus;
import com.reecho.reechobe.channel.domain.ChannelStatus;
import com.reecho.reechobe.channel.domain.ChannelVisibility;
import com.reecho.reechobe.channel.repository.ChannelMembershipRepository;
import com.reecho.reechobe.channel.repository.ChannelRepository;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.user.domain.User;
import com.reecho.reechobe.user.repository.UserRepository;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import com.reecho.reechobe.workspace.dto.CreateWorkspaceRequest;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
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
class WorkspaceCreateCommandServiceTest {

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

    private WorkspaceCreateCommandService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceCreateCommandService(
                userRepository,
                workspaceRepository,
                workspaceMembershipRepository,
                channelRepository,
                channelMembershipRepository
        );
    }

    @Test
    void 워크스페이스를_생성하고_OWNER와_general_채널을_함께_생성한다() {
        UUID userId = UUID.randomUUID();
        UUID profileImageFileId = UUID.randomUUID();
        User user = User.createActive("생성자", "https://example.com/profile.png");
        user.updateProfileImage("https://example.com/profile.png", profileImageFileId);
        ReflectionTestUtils.setField(user, "id", userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        CreateWorkspaceRequest request = new CreateWorkspaceRequest(
                " Re-Echo Team ",
                " 팀 워크스페이스 ",
                " https://example.com/workspace.png "
        );

        service.createWorkspace(userId, request);

        ArgumentCaptor<Workspace> workspaceCaptor = ArgumentCaptor.forClass(Workspace.class);
        ArgumentCaptor<WorkspaceMembership> membershipCaptor = ArgumentCaptor.forClass(WorkspaceMembership.class);
        ArgumentCaptor<Channel> channelCaptor = ArgumentCaptor.forClass(Channel.class);
        ArgumentCaptor<ChannelMembership> channelMembershipCaptor =
                ArgumentCaptor.forClass(ChannelMembership.class);

        verify(workspaceRepository).save(workspaceCaptor.capture());
        verify(workspaceMembershipRepository).save(membershipCaptor.capture());
        verify(channelRepository).save(channelCaptor.capture());
        verify(channelMembershipRepository).save(channelMembershipCaptor.capture());

        Workspace workspace = workspaceCaptor.getValue();
        WorkspaceMembership membership = membershipCaptor.getValue();
        Channel channel = channelCaptor.getValue();
        ChannelMembership channelMembership = channelMembershipCaptor.getValue();

        assertThat(workspace.getName()).isEqualTo("Re-Echo Team");
        assertThat(workspace.getDescription()).isEqualTo("팀 워크스페이스");
        assertThat(workspace.getImageUrl()).isEqualTo("https://example.com/workspace.png");
        assertThat(workspace.getCreatedByUserId()).isEqualTo(userId);
        assertThat(workspace.getStatus()).isEqualTo(WorkspaceStatus.ACTIVE);

        assertThat(membership.getWorkspaceId()).isEqualTo(workspace.getId());
        assertThat(membership.getUserId()).isEqualTo(userId);
        assertThat(membership.getRole()).isEqualTo(WorkspaceMembershipRole.OWNER);
        assertThat(membership.getDisplayName()).isEqualTo("생성자");
        assertThat(membership.getProfileImageUrl()).isEqualTo("https://example.com/profile.png");
        assertThat(membership.getProfileImageFileId()).isEqualTo(profileImageFileId);
        assertThat(membership.getStatus()).isEqualTo(WorkspaceMembershipStatus.ACTIVE);

        assertThat(channel.getWorkspaceId()).isEqualTo(workspace.getId());
        assertThat(channel.getName()).isEqualTo(Channel.GENERAL_CHANNEL_NAME);
        assertThat(channel.getVisibility()).isEqualTo(ChannelVisibility.PUBLIC);
        assertThat(channel.isGeneral()).isTrue();
        assertThat(channel.getCreatedByMembershipId()).isEqualTo(membership.getId());
        assertThat(channel.getStatus()).isEqualTo(ChannelStatus.ACTIVE);

        assertThat(channelMembership.getChannelId()).isEqualTo(channel.getId());
        assertThat(channelMembership.getWorkspaceMembershipId()).isEqualTo(membership.getId());
        assertThat(channelMembership.getStatus()).isEqualTo(ChannelMembershipStatus.ACTIVE);
    }

    @Test
    void 인증_사용자가_없으면_워크스페이스를_생성하지_않는다() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        CreateWorkspaceRequest request = new CreateWorkspaceRequest("Re-Echo Team", null, null);

        assertThatThrownBy(() -> service.createWorkspace(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTH_UNAUTHORIZED);

        verifyNoInteractions(
                workspaceRepository,
                workspaceMembershipRepository,
                channelRepository,
                channelMembershipRepository
        );
    }
}
