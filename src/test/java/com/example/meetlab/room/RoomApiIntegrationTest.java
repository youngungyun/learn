package com.example.meetlab.room;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.meetlab.TestcontainersConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.json.JsonMapper;

/**
 * 컨트롤러 -> 서비스 -> JPA -> PostgreSQL 까지 실제 HTTP 로 통과시키는 통합 테스트.
 * 슬라이스 테스트가 놓치는 "조립" 문제(빈 설정, 마이그레이션, 직렬화)를 잡는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@DisplayName("방 API 통합 테스트")
class RoomApiIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    private HttpResponse<String> post(String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/rooms"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("방을 만들면 DB 에 저장되고 코드로 다시 조회된다")
    void createsAndFetchesRoom() throws Exception {
        HttpResponse<String> created = post("{\"name\":\"통합 테스트 방\",\"maxParticipants\":3}");

        assertThat(created.statusCode()).isEqualTo(201);
        String code = JSON.readTree(created.body()).get("code").toString().replace("\"", "");
        assertThat(created.headers().firstValue("Location")).hasValue("/api/rooms/" + code);

        HttpResponse<String> fetched = get("/api/rooms/" + code);

        assertThat(fetched.statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(fetched.body()).get("maxParticipants").toString()).isEqualTo("3");
    }

    @Test
    @DisplayName("없는 방은 404")
    void unknownRoomIs404() throws Exception {
        assertThat(get("/api/rooms/zzz-zzzz-zzz").statusCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("이름이 비어 있거나 인원이 범위를 벗어나면 400")
    void invalidRequestIs400() throws Exception {
        assertThat(post("{\"name\":\"  \"}").statusCode()).isEqualTo(400);
        assertThat(post("{\"name\":\"x\",\"maxParticipants\":9}").statusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("최근 방 목록에 방금 만든 방이 들어 있다")
    void recentContainsCreatedRoom() throws Exception {
        post("{\"name\":\"최근 목록 확인용\"}");

        assertThat(get("/api/rooms").body()).contains("최근 목록 확인용");
    }
}
