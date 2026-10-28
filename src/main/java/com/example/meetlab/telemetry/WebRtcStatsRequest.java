package com.example.meetlab.telemetry;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 브라우저가 RTCPeerConnection.getStats() 로 뽑아 주기적으로 보내는 통화 품질 요약.
 *
 * <p>값의 범위와 candidateType 을 엄격히 검증하는 이유: 이 값이 그대로 지표(태그)가 되므로
 * 임의의 문자열을 받으면 시계열이 무한히 늘어난다.
 *
 * @param candidateType 실제로 쓰인 연결 경로. host(같은 망), srflx(STUN), prflx, relay(TURN 중계)
 * @param rttMs 왕복 지연(ms)
 * @param packetLossRatio 수신 패킷 손실 비율(0~1)
 * @param jitterMs 지터(ms)
 */
public record WebRtcStatsRequest(
        @NotNull @Pattern(regexp = "host|srflx|prflx|relay") String candidateType,
        @NotNull @DecimalMin("0") @DecimalMax("10000") Double rttMs,
        @NotNull @DecimalMin("0") @DecimalMax("1") Double packetLossRatio,
        @NotNull @DecimalMin("0") @DecimalMax("10000") Double jitterMs) {
}
