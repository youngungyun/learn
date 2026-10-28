package com.example.meetlab;

import java.nio.file.Path;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * nori 플러그인이 설치된 Elasticsearch 컨테이너를 JVM 당 한 번만 띄워 모든 테스트가 공유한다.
 *
 * <p>ES 는 기동이 느리고 메모리를 많이 쓰므로 테스트 클래스마다 새로 띄우면 빌드가 매우 느려진다.
 * 이미지는 compose 가 쓰는 {@code elasticsearch/Dockerfile} 을 그대로 빌드해서 쓴다.
 * 종료는 Testcontainers 의 Ryuk 가 JVM 이 끝난 뒤 정리한다.
 */
final class ElasticsearchTestContainer {

    private static final int HTTP_PORT = 9200;
    private static GenericContainer<?> container;

    private ElasticsearchTestContainer() {
    }

    /** 컨테이너를 (필요하면) 시작하고 접속 주소(http://host:port)를 돌려준다. */
    static synchronized String httpUri() {
        if (container == null) {
            GenericContainer<?> created = create();
            created.start();
            container = created;
        }
        return "http://" + container.getHost() + ":" + container.getMappedPort(HTTP_PORT);
    }

    private static GenericContainer<?> create() {
        ImageFromDockerfile image = new ImageFromDockerfile("meetlab-elasticsearch-test", false)
                .withFileFromPath("Dockerfile", Path.of("elasticsearch", "Dockerfile"));
        return new GenericContainer<>(image)
                .withEnv("discovery.type", "single-node")
                // 로컬 테스트 전용: 보안(인증/TLS)을 끈다. 운영에서는 절대 이렇게 두지 않는다.
                .withEnv("xpack.security.enabled", "false")
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                .withExposedPorts(HTTP_PORT)
                .waitingFor(Wait.forHttp("/_cluster/health?wait_for_status=yellow&timeout=60s")
                        .forPort(HTTP_PORT)
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(4)));
    }
}
