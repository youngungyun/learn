package com.example.meetlab.room.web;

import com.example.meetlab.room.domain.Room;
import java.time.Instant;

public record RoomResponse(String code, String name, int maxParticipants, Instant createdAt) {

    public static RoomResponse from(Room room) {
        return new RoomResponse(room.getCode(), room.getName(), room.getMaxParticipants(), room.getCreatedAt());
    }
}
