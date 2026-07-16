package com.reecho.reechobe.file.controller;

import com.reecho.reechobe.common.response.ApiResponse;
import com.reecho.reechobe.file.dto.MessageAttachmentPresignRequest;
import com.reecho.reechobe.file.dto.PresignedDownloadResponse;
import com.reecho.reechobe.file.dto.PresignedUploadResponse;
import com.reecho.reechobe.file.service.MessageAttachmentFileService;
import com.reecho.reechobe.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 메시지 첨부의 직접 업로드와 다운로드 URL 발급 API를 제공한다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceId}/files")
public class FileController {

    private final MessageAttachmentFileService messageAttachmentFileService;

    @PostMapping("/presign-upload")
    // 메시지 첨부 직접 업로드 URL 발급 요청을 파일 서비스에 위임한다.
    public ApiResponse<PresignedUploadResponse> createUploadUrl(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody MessageAttachmentPresignRequest request
    ) {
        PresignedUploadResponse result = messageAttachmentFileService.createUploadUrl(
                principal.userId(),
                workspaceId,
                request
        );
        return ApiResponse.success(HttpStatus.OK, "첨부 파일 업로드 URL이 발급되었습니다.", result);
    }

    @GetMapping("/{fileId}/download-url")
    // 접근 가능한 메시지 첨부의 제한 시간 다운로드 URL 발급을 파일 서비스에 위임한다.
    public ApiResponse<PresignedDownloadResponse> createDownloadUrl(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @PathVariable UUID fileId
    ) {
        PresignedDownloadResponse result = messageAttachmentFileService.createDownloadUrl(
                principal.userId(),
                workspaceId,
                fileId
        );
        return ApiResponse.success(HttpStatus.OK, "첨부 파일 다운로드 URL이 발급되었습니다.", result);
    }
}
