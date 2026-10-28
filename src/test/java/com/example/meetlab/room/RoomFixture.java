package com.example.meetlab.room;

import com.example.meetlab.room.domain.Room;
import java.time.Instant;

/** 테스트에서 Room을 짧게 만들기 위한 헬퍼. */
public final class RoomFixture {

    public static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private RoomFixture() {
    }

    public static Room room(String code, String name, int maxParticipants) {
        return new Room(code, name, maxParticipants, NOW);
    }

    public static Room room(String code) {
        return room(code, "주간 회의", 4);
    }
}
