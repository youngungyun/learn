package com.example.meetlab.ice;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * 브라우저에 내려줄 ICE 서버 목록을 만든다.
 *
 * <p>TURN 은 "TURN REST API" 방식의 임시 자격 증명을 쓴다.
 * 사용자 이름은 {@code 만료시각(epoch 초):사용자ID}, 비밀번호는 {@code Base64(HMAC-SHA1(공유 비밀, 사용자 이름))} 이다.
 * coturn 이 같은 공유 비밀로 직접 검증하므로 서버가 자격 증명을 저장할 필요가 없고, 만료 후에는 쓸 수 없다.
 */
@Service
public class IceServerService {

    private static final String HMAC_ALGORITHM = "HmacSHA1";

    private final IceProperties properties;
    private final Clock clock;

    public IceServerService(IceProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public IceServersResponse issue(String userId) {
        List<IceServer> servers = new ArrayList<>();
        if (!properties.stunUrls().isEmpty()) {
            servers.add(new IceServer(properties.stunUrls(), null, null));
        }

        Instant expiresAt = clock.instant().plus(properties.credentialTtl());
        if (isTurnConfigured()) {
            String username = expiresAt.getEpochSecond() + ":" + userId;
            servers.add(new IceServer(properties.turnUrls(), username, sign(username)));
        }
        return new IceServersResponse(List.copyOf(servers), expiresAt);
    }

    private boolean isTurnConfigured() {
        String secret = properties.turnSecret();
        return !properties.turnUrls().isEmpty() && secret != null && !secret.isBlank();
    }

    private String sign(String username) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(properties.turnSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(username.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("TURN 자격 증명 서명에 실패했습니다.", e);
        }
    }
}
