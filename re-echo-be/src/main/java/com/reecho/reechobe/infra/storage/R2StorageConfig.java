package com.reecho.reechobe.infra.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(R2StorageProperties.class)
public class R2StorageConfig {
}
