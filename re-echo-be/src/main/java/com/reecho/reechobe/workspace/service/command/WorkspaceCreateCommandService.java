package com.reecho.reechobe.workspace.service.command;

import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.channel.domain.Channel;
import com.reecho.reechobe.channel.domain.ChannelMembership;
import com.reecho.reechobe.channel.repository.ChannelMembershipRepository;
import com.reecho.reechobe.channel.repository.ChannelRepository;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.user.domain.User;
import com.reecho.reechobe.user.repository.UserRepository;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.dto.CreateWorkspaceRequest;
import com.reecho.reechobe.workspace.dto.CreatedWorkspaceResponse;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 워크스페이스 생성과 초기 기본 채널 구성을 하나의 트랜잭션으로 처리한다.
@Service
@RequiredArgsConstructor
public class WorkspaceCreateCommandService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMembershipRepository channelMembershipRepository;

    @Transactional
    public CreatedWorkspaceResponse createWorkspace(UUID userId, CreateWorkspaceRequest request) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_UNAUTHORIZED));

        Workspace workspace = Workspace.create(
                request.name(),
                request.description(),
                null,
                creator.getId()
        );
        workspaceRepository.save(workspace);

        WorkspaceMembership ownerMembership = WorkspaceMembership.createOwner(workspace.getId(), creator);
        workspaceMembershipRepository.save(ownerMembership);

        Channel generalChannel = Channel.createGeneral(workspace.getId(), ownerMembership.getId());
        channelRepository.save(generalChannel);

        ChannelMembership channelMembership = ChannelMembership.join(
                generalChannel.getId(),
                ownerMembership.getId()
        );
        channelMembershipRepository.save(channelMembership);

        return new CreatedWorkspaceResponse(workspace.getId(), generalChannel.getId());
    }
}
