package com.example.meetlab.signaling;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.meetlab.signaling.RoomSessionRegistry.Member;
import org.junit.jupiter.api.Test;

class RoomSessionRegistryTest {

    private final RoomSessionRegistry registry = new RoomSessionRegistry();

    private static Member member(String id) {
        return new Member(new FakeConnection(id), id);
    }

    @Test
    void rejectsJoinBeyondCapacity() {
        assertThat(registry.join("r", 2, member("a"))).isPresent();
        assertThat(registry.join("r", 2, member("b"))).isPresent();

        assertThat(registry.join("r", 2, member("c"))).isEmpty();
        assertThat(registry.sizeOf("r")).isEqualTo(2);
    }

    @Test
    void rejectsSamePeerJoiningTwice() {
        registry.join("r1", 4, member("a"));

        assertThat(registry.join("r2", 4, member("a"))).isEmpty();
    }

    @Test
    void joinReturnsPeopleAlreadyInRoom() {
        registry.join("r", 4, member("a"));

        var result = registry.join("r", 4, member("b")).orElseThrow();

        assertThat(result.existing()).extracting(m -> m.connection().id()).containsExactly("a");
    }

    @Test
    void leaveReturnsRemainingAndFreesSlot() {
        registry.join("r", 2, member("a"));
        registry.join("r", 2, member("b"));

        var remaining = registry.leave("a").orElseThrow();

        assertThat(remaining).extracting(m -> m.connection().id()).containsExactly("b");
        assertThat(registry.join("r", 2, member("c"))).isPresent();
    }

    @Test
    void leaveIsIdempotentAndRemovesEmptyRoom() {
        registry.join("r", 2, member("a"));

        assertThat(registry.leave("a")).isPresent();
        assertThat(registry.leave("a")).isEmpty();
        assertThat(registry.sizeOf("r")).isZero();
        assertThat(registry.totalPeers()).isZero();
    }

    @Test
    void findInSameRoomOnlyWithinRoom() {
        registry.join("r1", 4, member("a"));
        registry.join("r1", 4, member("b"));
        registry.join("r2", 4, member("c"));

        assertThat(registry.findInSameRoom("a", "b")).isPresent();
        assertThat(registry.findInSameRoom("a", "c")).isEmpty();
        assertThat(registry.findInSameRoom("a", "zzz")).isEmpty();
        assertThat(registry.findInSameRoom("zzz", "a")).isEmpty();
    }
}
