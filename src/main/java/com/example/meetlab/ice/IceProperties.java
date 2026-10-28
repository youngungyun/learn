package com.example.meetlab.ice;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * ICE(STUN/TURN) 서버 설정.
 *
 * @param stunUrls 공인 주소 확인용 STUN 서버 주소 목록
 * @param turnUrls 미디어 중계용 TURN 서버 주소 목록. 비어 있으면 TURN을 쓰지 않는다.
 * @param turnSecret coturn의 static-auth-secret 과 같은 값. 비어 있으면 TURN을 쓰지 않는다.
 * @param credentialTtl TURN 임시 자격 증명의 유효 시간
 */
@ConfigurationProperties(prefix = "meetlab.ice")
public record IceProperties(
        @DefaultValue("stun:stun.l.google.com:19302") List<String> stunUrls,
        @DefaultValue List<String> turnUrls,
        String turnSecret,
        @DefaultValue("1h") Duration credentialTtl) {
}
