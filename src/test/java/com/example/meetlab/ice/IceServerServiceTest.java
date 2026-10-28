package com.example.meetlab.ice;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class IceServerServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-29T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static IceProperties props(List<String> turnUrls, String secret) {
        return new IceProperties(List.of("stun:stun.example.com:3478"), turnUrls, secret, Duration.ofHours(1));
    }

    @Test
    void turnCredentialIsHmacSha1OfUsernameAndExpiresInTtl() throws Exception {
        IceServerService service = new IceServerService(props(List.of("turn:turn.example.com:3478"), "s3cret"), CLOCK);

        IceServersResponse response = service.issue("alice");

        assertThat(response.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
        IceServer turn = response.iceServers().get(1);
        assertThat(turn.username()).isEqualTo((NOW.getEpochSecond() + 3600) + ":alice");

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec("s3cret".getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] signature = mac.doFinal(turn.username().getBytes(StandardCharsets.UTF_8));
        String expected = Base64.getEncoder().encodeToString(signature);
        assertThat(turn.credential()).isEqualTo(expected);
    }

    @Test
    void stunEntryHasNoCredentials() {
        IceServerService service = new IceServerService(props(List.of("turn:t:3478"), "s"), CLOCK);

        IceServer stun = service.issue("bob").iceServers().get(0);

        assertThat(stun.urls()).containsExactly("stun:stun.example.com:3478");
        assertThat(stun.username()).isNull();
        assertThat(stun.credential()).isNull();
    }

    @Test
    void noTurnEntryWhenSecretBlankOrUrlsEmpty() {
        assertThat(new IceServerService(props(List.of("turn:t:3478"), " "), CLOCK).issue("a").iceServers()).hasSize(1);
        assertThat(new IceServerService(props(List.of("turn:t:3478"), null), CLOCK).issue("a").iceServers()).hasSize(1);
        assertThat(new IceServerService(props(List.of(), "s"), CLOCK).issue("a").iceServers()).hasSize(1);
    }
}
