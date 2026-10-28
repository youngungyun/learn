package com.example.meetlab;

import org.springframework.boot.SpringApplication;

/**
 * 로컬에서 {@code ./gradlew bootTestRun} 으로 실행하면 docker compose 없이도
 * Testcontainers 의 PostgreSQL 이 함께 떠서 앱을 바로 띄울 수 있다.
 */
public class TestMeetlabApplication {

    public static void main(String[] args) {
        SpringApplication.from(MeetlabApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
