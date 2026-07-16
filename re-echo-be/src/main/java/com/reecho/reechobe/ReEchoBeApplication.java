package com.reecho.reechobe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Re-Echo 백엔드의 Spring Boot 자동 구성과 실행을 시작한다.
@SpringBootApplication
public class ReEchoBeApplication {

    // 애플리케이션 컨텍스트를 생성해 HTTP와 비동기 구성 요소를 함께 기동한다.
    public static void main(String[] args) {
        SpringApplication.run(ReEchoBeApplication.class, args);
    }

}
