package com.reecho.reechobe.workspace.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reecho.reechobe.channel.domain.Channel;
import com.reecho.reechobe.channel.repository.ChannelRepository;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.user.domain.User;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import com.reecho.reechobe.workspace.dto.WorkspaceDetailResponse;
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
class WorkspaceQueryServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMembershipRepository workspaceMembershipRepository;

    @Mock
    private ChannelRepository channelRepository;

    private WorkspaceQueryService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceQueryService(
                workspaceRepository,
                workspaceMembershipRepository,
                channelRepository
        );
    }

    @Test
    void 워크스페이스_상세와_내_멤버십_기본_채널을_조회한다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        WorkspaceMembership membership = createOwnerMembership(workspace.getId(), userId);
        Channel generalChannel = Channel.createGeneral(workspace.getId(), membership.getId());
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(membership));
        when(channelRepository.findByWorkspaceIdAndGeneralTrue(workspace.getId()))
                .thenReturn(Optional.of(generalChannel));

        WorkspaceDetailResponse result = service.getWorkspaceDetail(userId, workspace.getId());

        assertThat(result.id()).isEqualTo(workspace.getId());
        assertThat(result.name()).isEqualTo("Re-Echo Team");
        assertThat(result.description()).isEqualTo("팀 워크스페이스");
        assertThat(result.status()).isEqualTo(WorkspaceStatus.ACTIVE);
        assertThat(result.defaultChannelId()).isEqualTo(generalChannel.getId());
        assertThat(result.canRestore()).isFalse();
        assertThat(result.myMembership().id()).isEqualTo(membership.getId());
        assertThat(result.myMembership().displayName()).isEqualTo("사용자");
        assertThat(result.myMembership().status()).isEqualTo(WorkspaceMembershipStatus.ACTIVE);
    }

    @Test
    void 보관된_워크스페이스의_OWNER는_만료_전이면_복원_가능하다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(userId);
        ReflectionTestUtils.setField(workspace, "status", WorkspaceStatus.ARCHIVED);
        ReflectionTestUtils.setField(workspace, "archiveExpiresAt", LocalDateTime.now().plusDays(3));
        WorkspaceMembership membership = createOwnerMembership(workspace.getId(), userId);
        Channel generalChannel = Channel.createGeneral(workspace.getId(), membership.getId());
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(membership));
        when(channelRepository.findByWorkspaceIdAndGeneralTrue(workspace.getId()))
                .thenReturn(Optional.of(generalChannel));

        WorkspaceDetailResponse result = service.getWorkspaceDetail(userId, workspace.getId());

        assertThat(result.status()).isEqualTo(WorkspaceStatus.ARCHIVED);
        assertThat(result.canRestore()).isTrue();
    }

    @Test
    void 워크스페이스가_없으면_조회_예외를_던진다() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getWorkspaceDetail(userId, workspaceId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);

        verifyNoInteractions(workspaceMembershipRepository, channelRepository);
    }

    @Test
    void 활성_멤버십이_없으면_접근_거부_예외를_던진다() {
        UUID userId = UUID.randomUUID();
        Workspace workspace = createWorkspace(UUID.randomUUID());
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(),
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getWorkspaceDetail(userId, workspace.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED);

        verifyNoInteractions(channelRepository);
    }

    @Test
    void 소유_워크스페이스와_참여_워크스페이스의_역할을_목록에_반환한다() {
        UUID userId = UUID.randomUUID();
        Workspace ownedWorkspace = createWorkspace(userId);
        Workspace joinedWorkspace = createWorkspace(UUID.randomUUID());
        WorkspaceMembership ownerMembership = createOwnerMembership(ownedWorkspace.getId(), userId);
        WorkspaceMembership memberMembership = createOwnerMembership(joinedWorkspace.getId(), userId);
        ReflectionTestUtils.setField(memberMembership, "role", WorkspaceMembershipRole.MEMBER);
        ReflectionTestUtils.setField(ownerMembership, "lastVisitedAt", LocalDateTime.now());
        ReflectionTestUtils.setField(memberMembership, "lastVisitedAt", LocalDateTime.now().minusDays(1));
        Channel ownedGeneralChannel = Channel.createGeneral(ownedWorkspace.getId(), ownerMembership.getId());
        Channel joinedGeneralChannel = Channel.createGeneral(joinedWorkspace.getId(), memberMembership.getId());
        when(workspaceMembershipRepository.findByUserIdAndStatusOrderByLastVisitedAtDesc(
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(List.of(ownerMembership, memberMembership));
        when(workspaceRepository.findAllById(List.of(ownedWorkspace.getId(), joinedWorkspace.getId())))
                .thenReturn(List.of(ownedWorkspace, joinedWorkspace));
        when(channelRepository.findByWorkspaceIdInAndGeneralTrue(
                List.of(ownedWorkspace.getId(), joinedWorkspace.getId())
        )).thenReturn(List.of(ownedGeneralChannel, joinedGeneralChannel));

        var result = service.getWorkspaceList(userId);

        assertThat(result.contents()).extracting("id")
                .containsExactly(ownedWorkspace.getId(), joinedWorkspace.getId());
        assertThat(result.contents()).extracting("role")
                .containsExactly(WorkspaceMembershipRole.OWNER, WorkspaceMembershipRole.MEMBER);
        assertThat(result.contents()).extracting("defaultChannelId")
                .containsExactly(ownedGeneralChannel.getId(), joinedGeneralChannel.getId());
        assertThat(result.contents()).extracting("unreadChannelCount")
                .containsOnly(0);
    }

    @Test
    void 활성_워크스페이스_멤버십이_없으면_빈_목록을_반환한다() {
        UUID userId = UUID.randomUUID();
        when(workspaceMembershipRepository.findByUserIdAndStatusOrderByLastVisitedAtDesc(
                userId,
                WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(List.of());

        var result = service.getWorkspaceList(userId);

        assertThat(result.contents()).isEmpty();
        verifyNoInteractions(workspaceRepository, channelRepository);
    }

    private Workspace createWorkspace(UUID createdByUserId) {
        return Workspace.create(
                "Re-Echo Team",
                "팀 워크스페이스",
                "https://example.com/workspace.png",
                createdByUserId
        );
    }

    private WorkspaceMembership createOwnerMembership(UUID workspaceId, UUID userId) {
        User user = User.createActive("사용자", "https://example.com/profile.png");
        ReflectionTestUtils.setField(user, "id", userId);
        return WorkspaceMembership.createOwner(workspaceId, user);
    }
}
