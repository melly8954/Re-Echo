package com.reecho.reechobe.infra.storage;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

// Cloudflare R2의 S3 호환 API를 사용해 presigned URL을 만든다.
@Component
@RequiredArgsConstructor
public class R2StorageClient implements StorageClient {

    private static final Region R2_REGION = Region.of("auto");

    private final R2StorageProperties properties;

    @Override
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
    public String publicUrl(String storageKey) {
        requireConfigured();
        return trimTrailingSlash(properties.publicBaseUrl()) + "/" + storageKey;
    }

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

    private StaticCredentialsProvider credentialsProvider() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.accessKey(),
                properties.secretKey()
        );
        return StaticCredentialsProvider.create(credentials);
    }

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
