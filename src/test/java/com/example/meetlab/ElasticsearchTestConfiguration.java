package com.example.meetlab;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

/**
 * 컨테이너가 뜬 뒤에야 알 수 있는 접속 주소를 스프링 설정에 주입한다.
 * PostgreSQL 은 {@code @ServiceConnection} 으로 자동 연결하지만, 직접 만든 이미지(nori 포함)를 쓰는 ES 는
 * 이렇게 속성을 직접 등록한다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ElasticsearchTestConfiguration {

    @Bean
    DynamicPropertyRegistrar elasticsearchProperties() {
        return registry -> registry.add("spring.elasticsearch.uris", ElasticsearchTestContainer::httpUri);
    }
}
