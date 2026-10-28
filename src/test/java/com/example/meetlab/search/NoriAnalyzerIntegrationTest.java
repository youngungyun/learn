package com.example.meetlab.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.meetlab.IntegrationTest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

/**
 * 분석기가 실제로 한국어를 나누는지 ES 의 _analyze API 로 확인한다.
 * 검색이 "안 나올 때" 가장 먼저 해 볼 디버깅이 바로 이 호출이다.
 */
@IntegrationTest
@DisplayName("nori 분석기")
class NoriAnalyzerIntegrationTest {

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUri;

    private String analyze(String analyzer, String text) throws Exception {
        String base = elasticsearchUri.split(",")[0];
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/rooms/_analyze"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"analyzer\":\"" + analyzer + "\",\"text\":\"" + text + "\"}"))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return response.body();
    }

    @Test
    @DisplayName("합성어 화상회의실을 화상 / 회의 / 실 로 나눈다")
    void decompoundsKoreanCompoundNoun() throws Exception {
        String body = analyze("korean", "화상회의실");

        assertThat(body).contains("\"token\":\"화상\"").contains("\"token\":\"회의\"");
    }

    @Test
    @DisplayName("조사는 걸러진다(회의를 → 회의)")
    void dropsParticles() throws Exception {
        String body = analyze("korean", "회의를");

        assertThat(body).contains("\"token\":\"회의\"").doesNotContain("\"token\":\"를\"");
    }

    @Test
    @DisplayName("name.prefix 분석기는 앞부분 조각(edge n-gram)을 만든다")
    void prefixAnalyzerEmitsEdgeNgrams() throws Exception {
        String body = analyze("korean_prefix", "room");

        assertThat(body).contains("\"token\":\"r\"").contains("\"token\":\"ro\"").contains("\"token\":\"room\"");
    }
}
