package com.example.meetlab;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * 전체 스택(PostgreSQL + Elasticsearch 컨테이너, 실제 HTTP 포트) 통합 테스트용 합성 애너테이션.
 * 모든 통합 테스트가 같은 설정을 쓰므로 스프링 컨텍스트와 컨테이너가 캐시되어 한 번만 뜬다.
 * 설정을 테스트마다 다르게 주면 컨텍스트가 새로 만들어져 느려지니 주의한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, ElasticsearchTestConfiguration.class})
public @interface IntegrationTest {
}
