package com.example.meetlab;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

@IntegrationTest
@DisplayName("Actuator")
class ActuatorIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("health 는 DB 까지 확인하고 UP 을 돌려준다")
    void healthIsUp() throws Exception {
        HttpResponse<String> response = get("/actuator/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("쿠버네티스용 liveness/readiness 프로브가 열려 있다")
    void probesAreExposed() throws Exception {
        assertThat(get("/actuator/health/liveness").statusCode()).isEqualTo(200);
        assertThat(get("/actuator/health/readiness").statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("노출하지 않은 엔드포인트(env)는 열리지 않는다")
    void sensitiveEndpointsAreNotExposed() throws Exception {
        assertThat(get("/actuator/env").statusCode()).isEqualTo(404);
    }
}
