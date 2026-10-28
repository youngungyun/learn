package com.example.meetlab;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 테스트와 로컬 개발(bootTestRun)이 함께 쓰는 컨테이너 정의.
 *
 * <p>{@code @ServiceConnection} 덕분에 컨테이너의 접속 정보(URL/계정)가 자동으로 DataSource 에 연결된다.
 * 운영과 같은 PostgreSQL 18 이미지를 쓰는 것이 H2 를 쓰지 않는 이유다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:18");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }
}
