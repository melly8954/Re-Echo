package com.reecho.reechobe.infra.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// R2 클라이언트가 사용할 외부 스토리지 설정을 한 번에 바인딩한다.
@Configuration
@EnableConfigurationProperties(R2StorageProperties.class)
public class R2StorageConfig {
}
