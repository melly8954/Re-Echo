package com.reecho.reechobe.message.controller;

import com.reecho.reechobe.common.response.ApiResponse;
import com.reecho.reechobe.common.response.CursorPageResponse;
import com.reecho.reechobe.file.service.MessageAttachmentFileService;
import com.reecho.reechobe.message.dto.ChannelMessageResponse;
import com.reecho.reechobe.message.dto.CreateMessageRequest;
import com.reecho.reechobe.message.dto.MessageCursorResponse;
import com.reecho.reechobe.message.dto.UpdateMessageRequest;
import com.reecho.reechobe.message.service.command.MessageCommandService;
import com.reecho.reechobe.message.service.query.MessageQueryService;
import com.reecho.reechobe.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;

// 채널 메시지의 조회·작성·수정·삭제 REST API를 제공한다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceId}/channels/{channelId}/messages")
public class MessageController {

    private final MessageQueryService messageQueryService;
    private final MessageCommandService messageCommandService;
    private final MessageAttachmentFileService messageAttachmentFileService;

    // 채널 멤버가 최신 메시지 또는 다음 과거 메시지 페이지를 조회한다.
    @GetMapping
    public ApiResponse<CursorPageResponse<ChannelMessageResponse, MessageCursorResponse>> getMessages(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @PathVariable UUID channelId,
            @RequestParam(defaultValue = "30") @Min(1) @Max(100) int size,
            @RequestParam(required = false) LocalDateTime cursorCreatedAt,
            @RequestParam(required = false) UUID cursorMessageId
    ) {
        CursorPageResponse<ChannelMessageResponse, MessageCursorResponse> result = messageQueryService.getMessages(
                principal.userId(),
                workspaceId,
                channelId,
                size,
                cursorCreatedAt,
                cursorMessageId
        );
        return ApiResponse.success(HttpStatus.OK, "메시지 목록을 조회했습니다.", result);
    }

    // 채널 멤버가 텍스트와 업로드 완료 첨부 파일로 메시지를 작성한다.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChannelMessageResponse> createMessage(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @PathVariable UUID channelId,
            @Valid @RequestBody CreateMessageRequest request
    ) {
        messageAttachmentFileService.requireUploadedAttachments(
                principal.userId(),
                workspaceId,
                request.fileIds()
        );
        ChannelMessageResponse result = messageCommandService.createMessage(
                principal.userId(),
                workspaceId,
                channelId,
                request
        );
        return ApiResponse.success(HttpStatus.CREATED, "메시지를 전송했습니다.", result);
    }

    // 메시지 작성자가 본문과 첨부 파일 전체 목록을 수정한다.
    @PatchMapping("/{messageId}")
    public ApiResponse<ChannelMessageResponse> updateMessage(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @PathVariable UUID channelId,
            @PathVariable UUID messageId,
            @Valid @RequestBody UpdateMessageRequest request
    ) {
        messageAttachmentFileService.requireUploadedAttachments(
                principal.userId(),
                workspaceId,
                request.fileIds()
        );
        ChannelMessageResponse result = messageCommandService.updateMessage(
                principal.userId(),
                workspaceId,
                channelId,
                messageId,
                request
        );
        return ApiResponse.success(HttpStatus.OK, "메시지를 수정했습니다.", result);
    }

    // 작성자 또는 관리 권한자가 메시지를 삭제 흔적 상태로 전환한다.
    @DeleteMapping("/{messageId}")
    public ApiResponse<Void> deleteMessage(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @PathVariable UUID channelId,
            @PathVariable UUID messageId
    ) {
        messageCommandService.deleteMessage(principal.userId(), workspaceId, channelId, messageId);
        return ApiResponse.success(HttpStatus.OK, "메시지를 삭제했습니다.", null);
    }
}
