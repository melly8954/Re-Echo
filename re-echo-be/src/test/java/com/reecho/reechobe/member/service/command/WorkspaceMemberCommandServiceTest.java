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
import com.reecho.reechobe.file.service.ProfileImageFileService;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.dto.UpdateWorkspaceProfileRequest;
import com.reecho.reechobe.member.dto.WorkspaceMemberResponse;
import com.reecho.reechobe.member.exception.MemberErrorCode;
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
class WorkspaceMemberCommandServiceTest {

    @Mock
    private WorkspaceMembershipRepository workspaceMembershipRepository;

    @Mock
    private ChannelMembershipRepository channelMembershipRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private ProfileImageFileService profileImageFileService;

    private WorkspaceMemberCommandService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceMemberCommandService(
                workspaceMembershipRepository,
                channelMembershipRepository,
                workspaceRepository,
                profileImageFileService
        );
    }

    @Test
    void 활성_멤버는_자신의_워크스페이스_표시_이름을_수정할_수_있다() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        WorkspaceMembership membership = createMembership(
                workspaceId,
                userId,
                WorkspaceMembershipRole.MEMBER
        );
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(
                Workspace.create("워크스페이스", null, null, userId)
        ));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspaceId,
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(membership));
        UpdateWorkspaceProfileRequest request = new UpdateWorkspaceProfileRequest();
        request.setDisplayName("워크스페이스 이름");

        WorkspaceMemberResponse result = service.updateMyProfile(userId, workspaceId, request);

        assertThat(result.displayName()).isEqualTo("워크스페이스 이름");
        verifyNoInteractions(profileImageFileService);
    }

    @Test
    void 일반_멤버가_탈퇴하면_워크스페이스와_채널_멤버십이_함께_종료된다() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        stubActiveWorkspace(workspaceId, userId);
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
        stubActiveWorkspace(workspaceId, userId);
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
        stubActiveWorkspace(workspaceId, userId);
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
        stubActiveWorkspace(workspaceId, userId);
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

    @Test
    void 소유자가_일반_멤버를_관리자로_변경할_수_있다() {
        UUID workspaceId = UUID.randomUUID();
        stubActiveWorkspace(workspaceId, UUID.randomUUID());
        WorkspaceMembership owner = createMembership(
                workspaceId,
                UUID.randomUUID(),
                WorkspaceMembershipRole.OWNER
        );
        WorkspaceMembership member = createMembership(
                workspaceId,
                UUID.randomUUID(),
                WorkspaceMembershipRole.MEMBER
        );
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspaceId,
                owner.getUserId(),
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(owner));
        when(workspaceMembershipRepository.findByIdAndWorkspaceIdAndStatus(
                member.getId(),
                workspaceId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(member));

        service.changeMemberRole(
                owner.getUserId(),
                workspaceId,
                member.getId(),
                WorkspaceMembershipRole.ADMIN
        );

        assertThat(member.getRole()).isEqualTo(WorkspaceMembershipRole.ADMIN);
    }

    @Test
    void 소유자가_아닌_멤버는_역할을_변경할_수_없다() {
        UUID workspaceId = UUID.randomUUID();
        stubActiveWorkspace(workspaceId, UUID.randomUUID());
        WorkspaceMembership admin = createMembership(
                workspaceId,
                UUID.randomUUID(),
                WorkspaceMembershipRole.ADMIN
        );
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspaceId,
                admin.getUserId(),
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.changeMemberRole(
                admin.getUserId(),
                workspaceId,
                UUID.randomUUID(),
                WorkspaceMembershipRole.MEMBER
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED);
    }

    @Test
    void 관리자도_일반_멤버를_강제_제거하면_채널_멤버십이_함께_종료된다() {
        UUID workspaceId = UUID.randomUUID();
        stubActiveWorkspace(workspaceId, UUID.randomUUID());
        WorkspaceMembership admin = createMembership(
                workspaceId,
                UUID.randomUUID(),
                WorkspaceMembershipRole.ADMIN
        );
        WorkspaceMembership member = createMembership(
                workspaceId,
                UUID.randomUUID(),
                WorkspaceMembershipRole.MEMBER
        );
        ChannelMembership channelMembership = ChannelMembership.join(
                UUID.randomUUID(),
                member.getId()
        );
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspaceId,
                admin.getUserId(),
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(admin));
        when(workspaceMembershipRepository.findByIdAndWorkspaceIdAndStatus(
                member.getId(),
                workspaceId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(member));
        when(channelMembershipRepository.findByWorkspaceMembershipIdAndStatus(
                member.getId(),
                ChannelMembershipStatus.ACTIVE
        )).thenReturn(List.of(channelMembership));

        service.removeMember(admin.getUserId(), workspaceId, member.getId());

        assertThat(member.getStatus()).isEqualTo(WorkspaceMembershipStatus.REMOVED);
        assertThat(member.getRemovedAt()).isNotNull();
        assertThat(channelMembership.getStatus()).isEqualTo(ChannelMembershipStatus.LEFT);
    }

    @Test
    void 소유자는_강제_제거할_수_없다() {
        UUID workspaceId = UUID.randomUUID();
        stubActiveWorkspace(workspaceId, UUID.randomUUID());
        WorkspaceMembership admin = createMembership(
                workspaceId,
                UUID.randomUUID(),
                WorkspaceMembershipRole.ADMIN
        );
        WorkspaceMembership owner = createMembership(
                workspaceId,
                UUID.randomUUID(),
                WorkspaceMembershipRole.OWNER
        );
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspaceId,
                admin.getUserId(),
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(admin));
        when(workspaceMembershipRepository.findByIdAndWorkspaceIdAndStatus(
                owner.getId(),
                workspaceId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> service.removeMember(
                admin.getUserId(),
                workspaceId,
                owner.getId()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(MemberErrorCode.MEMBER_REMOVE_FORBIDDEN);
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

    private void stubActiveWorkspace(UUID workspaceId, UUID ownerUserId) {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(
                Workspace.create("워크스페이스", null, null, ownerUserId)
        ));
    }
}
