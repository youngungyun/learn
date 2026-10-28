package com.example.meetlab.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.meetlab.IntegrationTest;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * 진짜 Elasticsearch(nori 포함)에 색인하고 검색한다.
 * ES 는 "거의 실시간"(기본 1초마다 refresh)이라 색인 직후에는 검색되지 않을 수 있다.
 * 그래서 Thread.sleep 대신 Awaitility 로 "될 때까지 최대 N초" 기다린다.
 */
@IntegrationTest
@DisplayName("방 검색 통합 테스트")
class RoomSearchIntegrationTest {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private RoomSearchRepository searchRepository;

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void createRoom(String name) throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/rooms"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"" + name + "\"}"))
                .build());
        assertThat(response.statusCode()).isEqualTo(201);
    }

    private String search(String query) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/search/rooms?q=" + encoded))
                .build()).body();
    }

    /** 다른 테스트와 겹치지 않도록 매번 다른 영문 접미사를 붙인다. */
    private static String unique() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    @Test
    @DisplayName("방을 만들면 커밋 후 색인되어 한국어 단어로 검색된다")
    void createdRoomBecomesSearchable() throws Exception {
        String tag = unique();
        createRoom("화상 회의실 " + tag);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(search("회의실 " + tag)).contains(tag));
    }

    @Test
    @DisplayName("합성어를 나눠 검색한다(회의 로 화상회의실을 찾는다)")
    void findsByDecompoundedWord() throws Exception {
        String tag = unique();
        createRoom("화상회의실" + tag);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(search("회의 " + tag)).contains(tag));
    }

    @Test
    @DisplayName("앞부분만 입력해도 찾는다(자동완성용 name.prefix)")
    void findsByPrefix() throws Exception {
        String tag = unique();
        createRoom("prefixroom" + tag);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(search("prefixr")).contains(tag));
    }

    @Test
    @DisplayName("검색 인덱스가 비어도 재색인 API 로 DB 기준으로 복구된다")
    void reindexRestoresMissingDocuments() throws Exception {
        String tag = unique();
        createRoom("복구대상 " + tag);
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(search("복구대상 " + tag)).contains(tag));

        searchRepository.deleteAll();
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(search("복구대상 " + tag)).doesNotContain(tag));

        URI reindexUri = URI.create("http://localhost:" + port + "/api/search/rooms/reindex");
        HttpResponse<String> reindexed = send(HttpRequest.newBuilder(reindexUri)
                .POST(HttpRequest.BodyPublishers.noBody()).build());

        assertThat(reindexed.statusCode()).isEqualTo(200);
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(search("복구대상 " + tag)).contains(tag));
    }

    @Test
    @DisplayName("따옴표가 섞인 검색어도 질의가 깨지지 않고 200 으로 답한다")
    void quoteInQueryDoesNotBreakSearch() throws Exception {
        String encoded = URLEncoder.encode("a\"}},{\"match_all\":{}}", StandardCharsets.UTF_8);

        HttpResponse<String> response = send(HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/api/search/rooms?q=" + encoded)).build());

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("검색어가 비어 있으면 빈 결과")
    void blankQueryReturnsEmpty() throws Exception {
        assertThat(search("   ")).isEqualTo("[]");
    }
}
