package com.reecho.reechobe.member.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reecho.reechobe.channel.domain.ChannelMembership;
import com.reecho.reechobe.channel.domain.ChannelMembershipStatus;
import com.reecho.reechobe.channel.repository.ChannelMembershipRepository;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.exception.MemberErrorCode;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.user.domain.User;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
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
class WorkspaceMemberCommandServiceTest {

    @Mock
    private WorkspaceMembershipRepository workspaceMembershipRepository;

    @Mock
    private ChannelMembershipRepository channelMembershipRepository;

    private WorkspaceMemberCommandService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceMemberCommandService(
                workspaceMembershipRepository,
                channelMembershipRepository
        );
    }

    @Test
    void 일반_멤버가_탈퇴하면_워크스페이스와_채널_멤버십이_함께_종료된다() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        WorkspaceMembership membership = createMembership(
                workspaceId,
                userId,
                WorkspaceMembershipRole.MEMBER
        );
        ChannelMembership generalMembership = ChannelMembership.join(UUID.randomUUID(), membership.getId());
        ChannelMembership publicMembership = ChannelMembership.join(UUID.randomUUID(), membership.getId());
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspaceId,
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(membership));
        when(channelMembershipRepository.findByWorkspaceMembershipIdAndStatus(
                membership.getId(),
                ChannelMembershipStatus.ACTIVE
        )).thenReturn(List.of(generalMembership, publicMembership));

        service.leaveWorkspace(userId, workspaceId, membership.getId());

        assertThat(membership.getStatus()).isEqualTo(WorkspaceMembershipStatus.LEFT);
        assertThat(membership.getLeftAt()).isNotNull();
        assertThat(generalMembership.getStatus()).isEqualTo(ChannelMembershipStatus.LEFT);
        assertThat(publicMembership.getStatus()).isEqualTo(ChannelMembershipStatus.LEFT);
        verify(channelMembershipRepository).findByWorkspaceMembershipIdAndStatus(
                membership.getId(),
                ChannelMembershipStatus.ACTIVE
        );
    }

    @Test
    void 소유자는_워크스페이스에서_나갈_수_없다() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        WorkspaceMembership membership = createMembership(
                workspaceId,
                userId,
                WorkspaceMembershipRole.OWNER
        );
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspaceId,
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> service.leaveWorkspace(userId, workspaceId, membership.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MemberErrorCode.MEMBER_LAST_OWNER_LEAVE_FORBIDDEN);

        assertThat(membership.getStatus()).isEqualTo(WorkspaceMembershipStatus.ACTIVE);
        verifyNoInteractions(channelMembershipRepository);
    }

    @Test
    void 본인이_아닌_멤버십으로는_탈퇴할_수_없다() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        WorkspaceMembership membership = createMembership(
                workspaceId,
                userId,
                WorkspaceMembershipRole.MEMBER
        );
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspaceId,
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> service.leaveWorkspace(userId, workspaceId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);

        verifyNoInteractions(channelMembershipRepository);
    }

    @Test
    void 활성_멤버가_아니면_탈퇴할_수_없다() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspaceId,
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.leaveWorkspace(userId, workspaceId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED);

        verifyNoInteractions(channelMembershipRepository);
    }

    private WorkspaceMembership createMembership(
            UUID workspaceId,
            UUID userId,
            WorkspaceMembershipRole role
    ) {
        User user = User.createActive("멤버", null);
        ReflectionTestUtils.setField(user, "id", userId);
        WorkspaceMembership membership = role == WorkspaceMembershipRole.OWNER
                ? WorkspaceMembership.createOwner(workspaceId, user)
                : WorkspaceMembership.createMember(workspaceId, user);
        ReflectionTestUtils.setField(membership, "role", role);
        return membership;
    }
}
