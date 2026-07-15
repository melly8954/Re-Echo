package com.reecho.reechobe.message.dto;

import com.reecho.reechobe.file.domain.FileObject;
import java.util.UUID;

// 첨부 파일의 목록 표시와 이미지 미리보기 판단에 필요한 정보를 전달한다.
public record MessageAttachmentResponse(
        UUID fileId,
        String fileName,
        String contentType,
        long size,
        boolean previewImage
) {

    public static MessageAttachmentResponse from(FileObject fileObject) {
        return new MessageAttachmentResponse(
                fileObject.getId(),
                fileObject.getOriginalFilename(),
                fileObject.getContentType(),
                fileObject.getFileSizeBytes(),
                fileObject.getContentType().startsWith("image/")
        );
    }
}
