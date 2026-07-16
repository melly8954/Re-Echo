package com.reecho.reechobe.file.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// 파일 정리 작업과 정리 기준 설정을 Spring 컨테이너에 등록한다.
@Configuration
@EnableScheduling
@EnableConfigurationProperties(FileCleanupProperties.class)
public class FileCleanupConfig {
}
