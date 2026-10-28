package com.example.meetlab.signaling;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 시그널링 서버의 비즈니스 지표.
 *
 * <p>태그 값은 반드시 "닫힌 집합"이어야 한다. 방 코드나 peer id 처럼 값이 무한히 늘어나는 것을 태그로 쓰면
 * 시계열이 폭발해 Prometheus 가 느려진다(카디널리티 폭발). 그래서 클라이언트가 보낸 type 문자열도
 * 그대로 쓰지 않고 {@link #normalize(String)} 으로 알려진 값만 통과시킨다.
 */
@Component
public class SignalingMetrics {

    static final String UNKNOWN = "unknown";
    private static final Set<String> KNOWN_TYPES = Set.of(
            SignalingTypes.JOIN, SignalingTypes.LEAVE, SignalingTypes.OFFER,
            SignalingTypes.ANSWER, SignalingTypes.CANDIDATE);

    private final MeterRegistry registry;

    public SignalingMetrics(MeterRegistry registry, RoomSessionRegistry sessions) {
        this.registry = registry;
        Gauge.builder("meetlab.peers.active", sessions, RoomSessionRegistry::totalPeers)
                .description("현재 방에 들어가 있는 접속자 수")
                .register(registry);
        Gauge.builder("meetlab.rooms.active", sessions, RoomSessionRegistry::roomCount)
                .description("현재 접속자가 1명 이상인 방의 수")
                .register(registry);
    }

    /** 알려진 메시지 유형만 그대로 쓰고 나머지는 unknown 으로 묶는다. */
    static String normalize(String type) {
        return type != null && KNOWN_TYPES.contains(type) ? type : UNKNOWN;
    }

    /** 메시지 한 건의 처리 시간을 재고, 건수를 센다. */
    public void record(String type, Runnable handler) {
        String tag = normalize(type);
        registry.counter("meetlab.signaling.messages", "type", tag).increment();
        Timer.builder("meetlab.signaling.handle")
                .description("시그널링 메시지 처리 시간")
                .tag("type", tag)
                .serviceLevelObjectives(Duration.ofMillis(1), Duration.ofMillis(5), Duration.ofMillis(25),
                        Duration.ofMillis(100), Duration.ofMillis(500))
                .register(registry)
                .record(handler);
    }

    /** 입장이 거절된 이유별 건수. reason 은 코드에서 정한 고정 문자열만 쓴다. */
    public void joinRejected(String reason) {
        registry.counter("meetlab.join.rejections", "reason", reason).increment();
    }

    public void joinSucceeded() {
        registry.counter("meetlab.join.successes").increment();
    }
}
