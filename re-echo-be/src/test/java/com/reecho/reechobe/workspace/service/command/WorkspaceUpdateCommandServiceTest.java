package com.reecho.reechobe.workspace.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reecho.reechobe.channel.domain.Channel;
import com.reecho.reechobe.channel.repository.ChannelRepository;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.file.service.WorkspaceImageFileService;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.user.domain.User;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.dto.UpdateWorkspaceRequest;
import com.reecho.reechobe.workspace.dto.WorkspaceDetailResponse;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WorkspaceUpdateCommandServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMembershipRepository workspaceMembershipRepository;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private WorkspaceImageFileService workspaceImageFileService;

    private WorkspaceUpdateCommandService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceUpdateCommandService(
                workspaceRepository,
                workspaceMembershipRepository,
                channelRepository,
                workspaceImageFileService
        );
    }

    @Test
    void 관리자는_업로드가_완료된_대표_이미지와_기본_정보를_변경할_수_있다() {
        UUID userId = UUID.randomUUID();
        User user = User.createActive("관리자", null);
        ReflectionTestUtils.setField(user, "id", userId);
        Workspace workspace = Workspace.create("기존 이름", "기존 설명", null, userId);
        WorkspaceMembership membership = WorkspaceMembership.createOwner(workspace.getId(), user);
        UUID imageFileId = UUID.randomUUID();
        Channel generalChannel = Channel.createGeneral(workspace.getId(), membership.getId());
        UpdateWorkspaceRequest request = new UpdateWorkspaceRequest(
                "새 이름",
                "새 설명",
                imageFileId
        );
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(), userId, WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(membership));
        when(workspaceImageFileService.requireUploadedWorkspaceImageUrl(
                workspace.getId(), userId, imageFileId
        )).thenReturn("https://example.com/workspace.png");
        when(channelRepository.findByWorkspaceIdAndGeneralTrue(workspace.getId()))
                .thenReturn(Optional.of(generalChannel));

        WorkspaceDetailResponse result = service.updateWorkspace(userId, workspace.getId(), request);

        assertThat(workspace.getName()).isEqualTo("새 이름");
        assertThat(workspace.getDescription()).isEqualTo("새 설명");
        assertThat(workspace.getImageFileId()).isEqualTo(imageFileId);
        assertThat(workspace.getImageUrl()).isEqualTo("https://example.com/workspace.png");
        assertThat(result.defaultChannelId()).isEqualTo(generalChannel.getId());
    }

    @Test
    void 일반_멤버는_워크스페이스_설정을_변경할_수_없다() {
        UUID userId = UUID.randomUUID();
        User user = User.createActive("멤버", null);
        ReflectionTestUtils.setField(user, "id", userId);
        Workspace workspace = Workspace.create("워크스페이스", null, null, userId);
        WorkspaceMembership membership = WorkspaceMembership.createMember(workspace.getId(), user);
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(), userId, WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> service.updateWorkspace(
                userId,
                workspace.getId(),
                new UpdateWorkspaceRequest("새 이름", null, null)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED);

        verifyNoInteractions(workspaceImageFileService, channelRepository);
    }

    @Test
    void 대표_이미지를_교체하면_기존_파일을_고아_상태로_전환한다() {
        UUID userId = UUID.randomUUID();
        UUID previousImageFileId = UUID.randomUUID();
        UUID nextImageFileId = UUID.randomUUID();
        User user = User.createActive("소유자", null);
        ReflectionTestUtils.setField(user, "id", userId);
        Workspace workspace = Workspace.create("워크스페이스", null, null, userId);
        ReflectionTestUtils.setField(workspace, "imageFileId", previousImageFileId);
        WorkspaceMembership membership = WorkspaceMembership.createOwner(workspace.getId(), user);
        Channel generalChannel = Channel.createGeneral(workspace.getId(), membership.getId());
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                workspace.getId(), userId, WorkspaceMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(membership));
        when(workspaceImageFileService.requireUploadedWorkspaceImageUrl(
                workspace.getId(), userId, nextImageFileId
        )).thenReturn("https://example.com/next.png");
        when(channelRepository.findByWorkspaceIdAndGeneralTrue(workspace.getId()))
                .thenReturn(Optional.of(generalChannel));

        service.updateWorkspace(
                userId,
                workspace.getId(),
                new UpdateWorkspaceRequest("워크스페이스", null, nextImageFileId)
        );

        verify(workspaceImageFileService)
                .markWorkspaceImageOrphaned(workspace.getId(), previousImageFileId);
    }
}
