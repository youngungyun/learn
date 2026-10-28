package com.example.meetlab.telemetry;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** 브라우저가 보고한 통화 품질을 Micrometer 분포 요약(DistributionSummary)으로 기록한다. */
@Component
public class WebRtcTelemetry {

    private static final String TAG = "candidate_type";

    private final MeterRegistry registry;

    public WebRtcTelemetry(MeterRegistry registry) {
        this.registry = registry;
    }

    public void record(WebRtcStatsRequest stats) {
        String type = stats.candidateType();
        DistributionSummary.builder("meetlab.webrtc.rtt")
                .baseUnit("milliseconds")
                .description("브라우저가 보고한 WebRTC 왕복 지연")
                .tag(TAG, type)
                .serviceLevelObjectives(25, 50, 100, 200, 400, 800)
                .register(registry)
                .record(stats.rttMs());
        DistributionSummary.builder("meetlab.webrtc.jitter")
                .baseUnit("milliseconds")
                .tag(TAG, type)
                .serviceLevelObjectives(5, 10, 20, 40, 80)
                .register(registry)
                .record(stats.jitterMs());
        DistributionSummary.builder("meetlab.webrtc.packet.loss")
                .description("수신 패킷 손실 비율(0~1)")
                .tag(TAG, type)
                .serviceLevelObjectives(0.005, 0.01, 0.02, 0.05, 0.1)
                .register(registry)
                .record(stats.packetLossRatio());
    }
}
