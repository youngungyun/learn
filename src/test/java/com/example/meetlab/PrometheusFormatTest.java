package com.example.meetlab;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.meetlab.signaling.RoomSessionRegistry;
import com.example.meetlab.signaling.SignalingMetrics;
import com.example.meetlab.telemetry.WebRtcStatsRequest;
import com.example.meetlab.telemetry.WebRtcTelemetry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;

/**
 * Micrometer 지표 이름이 Prometheus 에서 어떤 이름으로 바뀌는지 눈으로 확인하는 학습용 테스트.
 * 점(.)은 밑줄(_)이 되고, 카운터에는 _total, 단위가 있으면 단위가 붙는다.
 * Grafana 쿼리(PromQL)는 이 변환된 이름을 쓴다.
 */
class PrometheusFormatTest {

    private final PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    @Test
    void metricNamesFollowPrometheusConventions() {
        RoomSessionRegistry sessions = new RoomSessionRegistry();
        SignalingMetrics metrics = new SignalingMetrics(registry, sessions);
        metrics.record("offer", () -> { });
        metrics.joinRejected("room_not_found");
        new WebRtcTelemetry(registry).record(new WebRtcStatsRequest("relay", 90.0, 0.01, 8.0));

        String scrape = registry.scrape();

        assertThat(scrape)
                .contains("meetlab_peers_active")
                .contains("meetlab_rooms_active")
                .contains("meetlab_signaling_messages_total{type=\"offer\"}")
                .contains("meetlab_join_rejections_total{reason=\"room_not_found\"}")
                .contains("meetlab_signaling_handle_seconds_bucket")
                .contains("meetlab_webrtc_rtt_milliseconds_bucket{candidate_type=\"relay\"");
    }
}
