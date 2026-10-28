package com.example.meetlab.signaling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.example.meetlab.room.domain.Room;
import com.example.meetlab.room.exception.RoomNotFoundException;
import com.example.meetlab.room.service.RoomService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class SignalingServiceTest {

    private static final String ROOM = "abc-defg-hij";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private RoomService roomService;
    private SignalingService service;
    private RoomSessionRegistry registry;
    private SimpleMeterRegistry meters;

    @BeforeEach
    void setUp() {
        roomService = mock(RoomService.class);
        given(roomService.get(ROOM)).willReturn(new Room(ROOM, "demo", 2, Instant.parse("2026-10-29T00:00:00Z")));
        given(roomService.get("nope-nope-non")).willThrow(new RoomNotFoundException("nope-nope-non"));
        registry = new RoomSessionRegistry();
        meters = new SimpleMeterRegistry();
        service = new SignalingService(roomService, registry, new SignalingMetrics(meters, registry));
    }

    private static ClientMessage join(String room, String name) {
        return new ClientMessage(SignalingTypes.JOIN, room, name, null, null);
    }

    private static JsonNode sdp() {
        return JSON.readTree("{\"sdp\":\"v=0\"}");
    }

    @Test
    void firstJoinerGetsEmptyPeerList() {
        FakeConnection a = new FakeConnection("a");

        service.handle(a, join(ROOM, "Alice"));

        assertThat(a.last().type()).isEqualTo(SignalingTypes.JOINED);
        assertThat(a.last().id()).isEqualTo("a");
        assertThat(a.last().peers()).isEmpty();
    }

    @Test
    void secondJoinerSeesFirstAndFirstIsNotified() {
        FakeConnection a = new FakeConnection("a");
        FakeConnection b = new FakeConnection("b");
        service.handle(a, join(ROOM, "Alice"));

        service.handle(b, join(ROOM, "Bob"));

        assertThat(b.last().peers()).extracting(PeerInfo::id).containsExactly("a");
        assertThat(a.last().type()).isEqualTo(SignalingTypes.PEER_JOINED);
        assertThat(a.last().id()).isEqualTo("b");
        assertThat(a.last().displayName()).isEqualTo("Bob");
    }

    @Test
    void unknownRoomIsRejected() {
        FakeConnection a = new FakeConnection("a");

        service.handle(a, join("nope-nope-non", "Alice"));

        assertThat(a.last().type()).isEqualTo(SignalingTypes.ERROR);
        assertThat(a.last().code()).isEqualTo("ROOM_NOT_FOUND");
    }

    @Test
    void fullRoomIsRejected() {
        service.handle(new FakeConnection("a"), join(ROOM, "A"));
        service.handle(new FakeConnection("b"), join(ROOM, "B"));
        FakeConnection c = new FakeConnection("c");

        service.handle(c, join(ROOM, "C"));

        assertThat(c.last().code()).isEqualTo("JOIN_REJECTED");
    }

    @Test
    void blankNameFallsBackToGuestName() {
        FakeConnection a = new FakeConnection("abcdef");
        FakeConnection b = new FakeConnection("b");
        service.handle(b, join(ROOM, "Bob"));

        service.handle(a, join(ROOM, "  "));

        assertThat(b.last().displayName()).isEqualTo("guest-abcd");
    }

    @Test
    void offerIsRelayedWithServerFilledSender() {
        FakeConnection a = new FakeConnection("a");
        FakeConnection b = new FakeConnection("b");
        service.handle(a, join(ROOM, "A"));
        service.handle(b, join(ROOM, "B"));

        service.handle(b, new ClientMessage(SignalingTypes.OFFER, null, null, "a", sdp()));

        assertThat(a.last().type()).isEqualTo(SignalingTypes.OFFER);
        assertThat(a.last().from()).isEqualTo("b");
        assertThat(a.last().data()).isEqualTo(sdp());
    }

    @Test
    void relayToPeerInAnotherRoomIsRejected() {
        FakeConnection a = new FakeConnection("a");
        service.handle(a, join(ROOM, "A"));
        FakeConnection stranger = new FakeConnection("x");

        service.handle(stranger, new ClientMessage(SignalingTypes.CANDIDATE, null, null, "a", sdp()));

        assertThat(stranger.last().code()).isEqualTo("PEER_NOT_FOUND");
        assertThat(a.received()).hasSize(1); // joined 뿐
    }

    @Test
    void relayWithoutTargetOrDataIsBadRequest() {
        FakeConnection a = new FakeConnection("a");
        service.handle(a, join(ROOM, "A"));

        service.handle(a, new ClientMessage(SignalingTypes.OFFER, null, null, null, sdp()));
        assertThat(a.last().code()).isEqualTo("BAD_REQUEST");

        service.handle(a, new ClientMessage(SignalingTypes.OFFER, null, null, "b", null));
        assertThat(a.last().code()).isEqualTo("BAD_REQUEST");
    }

    @Test
    void disconnectNotifiesRemainingAndIsIdempotent() {
        FakeConnection a = new FakeConnection("a");
        FakeConnection b = new FakeConnection("b");
        service.handle(a, join(ROOM, "A"));
        service.handle(b, join(ROOM, "B"));
        int before = a.received().size();

        service.disconnected(b);
        service.disconnected(b);

        assertThat(a.received()).hasSize(before + 1);
        assertThat(a.last().type()).isEqualTo(SignalingTypes.PEER_LEFT);
        assertThat(a.last().id()).isEqualTo("b");
    }

    @Test
    void leaveMessageActsLikeDisconnect() {
        FakeConnection a = new FakeConnection("a");
        FakeConnection b = new FakeConnection("b");
        service.handle(a, join(ROOM, "A"));
        service.handle(b, join(ROOM, "B"));

        service.handle(b, new ClientMessage(SignalingTypes.LEAVE, null, null, null, null));

        assertThat(a.last().type()).isEqualTo(SignalingTypes.PEER_LEFT);
    }

    @Test
    void missingOrUnknownTypeReturnsError() {
        FakeConnection a = new FakeConnection("a");

        service.handle(a, new ClientMessage(null, null, null, null, null));
        assertThat(a.last().code()).isEqualTo("BAD_REQUEST");

        service.handle(a, new ClientMessage("dance", null, null, null, null));
        assertThat(a.last().code()).isEqualTo("UNKNOWN_TYPE");
    }

    @Test
    void recordsMessageCountsAndJoinOutcomes() {
        service.handle(new FakeConnection("a"), join(ROOM, "A"));
        service.handle(new FakeConnection("b"), join(ROOM, "B"));
        service.handle(new FakeConnection("c"), join(ROOM, "C"));
        service.handle(new FakeConnection("d"), join("nope-nope-non", "D"));

        assertThat(meters.get("meetlab.signaling.messages").tag("type", "join").counter().count()).isEqualTo(4.0);
        assertThat(meters.get("meetlab.join.successes").counter().count()).isEqualTo(2.0);
        assertThat(meters.get("meetlab.join.rejections").tag("reason", "full_or_duplicate").counter().count())
                .isEqualTo(1.0);
        assertThat(meters.get("meetlab.join.rejections").tag("reason", "room_not_found").counter().count())
                .isEqualTo(1.0);
        assertThat(meters.get("meetlab.signaling.handle").tag("type", "join").timer().count()).isEqualTo(4L);
    }

    @Test
    void gaugesFollowRegistryState() {
        FakeConnection a = new FakeConnection("a");
        service.handle(a, join(ROOM, "A"));
        service.handle(new FakeConnection("b"), join(ROOM, "B"));

        assertThat(meters.get("meetlab.peers.active").gauge().value()).isEqualTo(2.0);
        assertThat(meters.get("meetlab.rooms.active").gauge().value()).isEqualTo(1.0);

        service.disconnected(a);

        assertThat(meters.get("meetlab.peers.active").gauge().value()).isEqualTo(1.0);
    }

    @Test
    void unknownTypeIsBucketedToAvoidHighCardinalityTags() {
        FakeConnection a = new FakeConnection("a");

        service.handle(a, new ClientMessage("dance-1", null, null, null, null));
        service.handle(a, new ClientMessage("dance-2", null, null, null, null));

        assertThat(meters.get("meetlab.signaling.messages").tag("type", "unknown").counter().count()).isEqualTo(2.0);
        assertThat(meters.find("meetlab.signaling.messages").counters()).hasSize(1);
    }
}
