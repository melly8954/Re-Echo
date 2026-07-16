package com.reecho.reechobe.member.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
class WorkspaceMemberQueryServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMembershipRepository workspaceMembershipRepository;

    private WorkspaceMemberQueryService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceMemberQueryService(workspaceRepository, workspaceMembershipRepository);
    }

    @Test
    void 워크스페이스_활성_멤버_목록을_역할과_이름순으로_조회한다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace();
        WorkspaceMembership requester = createMembership(workspace.getId(), userId, "요청자", WorkspaceMembershipRole.MEMBER);
        WorkspaceMembership owner = createMembership(workspace.getId(), UUID.randomUUID(), "소유자", WorkspaceMembershipRole.OWNER);
        WorkspaceMembership admin = createMembership(workspace.getId(), UUID.randomUUID(), "관리자", WorkspaceMembershipRole.ADMIN);
        WorkspaceMembership member = createMembership(workspace.getId(), UUID.randomUUID(), "멤버", WorkspaceMembershipRole.MEMBER);
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(requester));
        when(workspaceMembershipRepository.findByWorkspaceIdAndStatus(
                workspace.getId(),
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(List.of(member, requester, owner, admin));

        var result = service.getWorkspaceMembers(userId, workspace.getId());

        assertThat(result.contents()).extracting("displayName")
                .containsExactly("소유자", "관리자", "멤버", "요청자");
        assertThat(result.contents()).extracting("role")
                .containsExactly(
                        WorkspaceMembershipRole.OWNER,
                        WorkspaceMembershipRole.ADMIN,
                        WorkspaceMembershipRole.MEMBER,
                        WorkspaceMembershipRole.MEMBER
                );
    }

    @Test
    void 워크스페이스가_없으면_멤버_목록을_조회할_수_없다() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getWorkspaceMembers(userId, workspaceId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);

        verifyNoInteractions(workspaceMembershipRepository);
    }

    @Test
    void 활성_멤버가_아니면_멤버_목록을_조회할_수_없다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace();
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getWorkspaceMembers(userId, workspace.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED);
    }

    private Workspace createWorkspace() {
        return Workspace.create("Re-Echo Team", null, null, UUID.randomUUID());
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
