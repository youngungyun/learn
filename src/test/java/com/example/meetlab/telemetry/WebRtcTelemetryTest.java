package com.example.meetlab.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class WebRtcTelemetryTest {

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final WebRtcTelemetry telemetry = new WebRtcTelemetry(meters);

    @Test
    void recordsQualityPerCandidateType() {
        telemetry.record(new WebRtcStatsRequest("relay", 120.0, 0.02, 15.0));
        telemetry.record(new WebRtcStatsRequest("relay", 80.0, 0.0, 5.0));
        telemetry.record(new WebRtcStatsRequest("host", 3.0, 0.0, 1.0));

        var relayRtt = meters.get("meetlab.webrtc.rtt").tag("candidate_type", "relay").summary();
        assertThat(relayRtt.count()).isEqualTo(2L);
        assertThat(relayRtt.totalAmount()).isEqualTo(200.0);
        assertThat(meters.get("meetlab.webrtc.rtt").tag("candidate_type", "host").summary().count()).isEqualTo(1L);
        assertThat(meters.get("meetlab.webrtc.packet.loss").tag("candidate_type", "relay").summary().max())
                .isEqualTo(0.02);
    }
}
