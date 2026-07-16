package com.reecho.reechobe.infra.storage;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

// Cloudflare R2의 S3 호환 API를 사용해 presigned URL을 만든다.
@Component
@RequiredArgsConstructor
public class R2StorageClient implements StorageClient {

    private static final Region R2_REGION = Region.of("auto");

    private final R2StorageProperties properties;

    @Override
    // 파일 바이너리는 클라이언트가 직접 전송하도록 제한 시간 업로드 URL만 발급한다.
    public PresignedUpload presignPut(String storageKey, String contentType, Duration ttl) {
        requireConfigured();
        try (S3Presigner presigner = createPresigner()) {
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(ttl)
                    .putObjectRequest(builder -> builder
                            .bucket(properties.bucket())
                            .key(storageKey)
                            .contentType(contentType)
                    )
                    .build();
            PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
            return new PresignedUpload(
                    presignedRequest.url().toString(),
                    Instant.now().plus(ttl)
            );
        }
    }

    @Override
    // 비공개 파일을 노출하지 않고 제한 시간 다운로드 URL을 발급한다.
    public PresignedDownload presignGet(String storageKey, Duration ttl) {
        requireConfigured();
        try (S3Presigner presigner = createPresigner()) {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(ttl)
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(properties.bucket())
                            .key(storageKey)
                            .build())
                    .build();
            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
            return new PresignedDownload(
                    presignedRequest.url().toString(),
                    Instant.now().plus(ttl)
            );
        }
    }

    @Override
    // 메타데이터 연결 전 외부 스토리지 객체가 실제로 생성되었는지 확인한다.
    public boolean exists(String storageKey) {
        requireConfigured();
        try (S3Client s3Client = createS3Client()) {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .build();
            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException exception) {
            return false;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return false;
            }
            throw exception;
        }
    }

    @Override
    // 파일 연결 전에 R2가 보고한 실제 객체 크기를 확인한다.
    public Optional<Long> findObjectSize(String storageKey) {
        requireConfigured();
        try (S3Client s3Client = createS3Client()) {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .build();
            return Optional.ofNullable(s3Client.headObject(request).contentLength());
        } catch (NoSuchKeyException exception) {
            return Optional.empty();
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    @Override
    // 정리 대상으로 확정된 객체만 R2에서 제거한다.
    public void delete(String storageKey) {
        requireConfigured();
        try (S3Client s3Client = createS3Client()) {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .build();
            s3Client.deleteObject(request);
        }
    }

    @Override
    // 공개 이미지 표시가 허용된 경우에만 사용할 외부 URL을 구성한다.
    public String publicUrl(String storageKey) {
        requireConfigured();
        return trimTrailingSlash(properties.publicBaseUrl()) + "/" + storageKey;
    }

    // R2의 S3 호환 endpoint와 path-style 요청 규칙으로 URL 서명기를 생성한다.
    private S3Presigner createPresigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .credentialsProvider(credentialsProvider())
                .region(R2_REGION)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    // 객체 존재 확인과 삭제에만 사용할 짧은 수명의 R2 클라이언트를 생성한다.
    private S3Client createS3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .credentialsProvider(credentialsProvider())
                .region(R2_REGION)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }

    // SDK 기본 자격 증명 탐색과 분리해 설정된 R2 키만 사용하도록 고정한다.
    private StaticCredentialsProvider credentialsProvider() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.accessKey(),
                properties.secretKey()
        );
        return StaticCredentialsProvider.create(credentials);
    }

    // 잘못된 설정으로 외부 요청을 보내기 전에 필수 R2 연결 정보를 검증한다.
    private void requireConfigured() {
        if (isBlank(properties.endpoint())
                || isBlank(properties.bucket())
                || isBlank(properties.accessKey())
                || isBlank(properties.secretKey())
                || isBlank(properties.publicBaseUrl())) {
            throw new IllegalStateException("R2 storage configuration is required.");
        }
    }

    private static String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
