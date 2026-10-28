package com.example.meetlab.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * ECS 구조화 로그가 실제로 어떤 JSON 을 내는지 확인한다. DB/ES 가 필요 없도록 빈 설정 클래스만 올리고,
 * 콘솔 출력을 가로채 한 줄씩 JSON 으로 해석한다. ELK 쪽 파이프라인(Filebeat/Logstash)은 이 모양을 전제로 한다.
 */
@SpringBootTest(
        classes = StructuredLoggingTest.EmptyConfig.class,
        properties = {
            "spring.main.web-application-type=none",
            "logging.structured.format.console=ecs",
            "logging.structured.ecs.service.name=meetlab-test"
        })
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("ECS 구조화 로그")
class StructuredLoggingTest {

    private static final Logger LOG = LoggerFactory.getLogger("meetlab.logging.probe");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Configuration(proxyBeanMethods = false)
    static class EmptyConfig {
    }

    /** ECS 필드는 "log.level" 처럼 점이 들어간 평평한 키이거나 중첩 객체일 수 있어 두 모양 모두 읽는다. */
    private static JsonNode field(JsonNode line, String dottedName) {
        JsonNode flat = line.get(dottedName);
        if (flat != null) {
            return flat;
        }
        JsonNode current = line;
        for (String part : dottedName.split("\\.")) {
            current = current == null ? null : current.get(part);
        }
        return current;
    }

    private static JsonNode lineContaining(CapturedOutput output, String marker) {
        List<String> lines = output.getAll().lines().filter(l -> l.contains(marker)).toList();
        assertThat(lines).as("'%s' 를 포함한 로그 줄", marker).isNotEmpty();
        return JSON.readTree(lines.get(0));
    }

    @Test
    @DisplayName("메시지·레벨·MDC·key-value 가 JSON 필드로 나온다")
    void emitsMessageLevelMdcAndKeyValues(CapturedOutput output) {
        MDC.put("request_id", "req-12345678");
        try {
            LOG.atInfo().addKeyValue("room", "abc-defg-hij").addKeyValue("event", "probe").log("probe-message-1");
        } finally {
            MDC.remove("request_id");
        }

        JsonNode line = lineContaining(output, "probe-message-1");

        assertThat(field(line, "message").toString()).contains("probe-message-1");
        assertThat(field(line, "log.level").toString()).contains("INFO");
        assertThat(field(line, "service.name").toString()).contains("meetlab-test");
        assertThat(field(line, "request_id").toString()).contains("req-12345678");
        assertThat(field(line, "room").toString()).contains("abc-defg-hij");
        assertThat(field(line, "@timestamp")).isNotNull();
    }

    @Test
    @DisplayName("예외는 error.* 필드로 구조화되어 나온다")
    void emitsExceptionAsErrorFields(CapturedOutput output) {
        LOG.error("probe-error-2", new IllegalStateException("boom"));

        JsonNode line = lineContaining(output, "probe-error-2");

        assertThat(field(line, "log.level").toString()).contains("ERROR");
        assertThat(field(line, "error.type").toString()).contains("IllegalStateException");
        assertThat(field(line, "error.message").toString()).contains("boom");
    }
}
