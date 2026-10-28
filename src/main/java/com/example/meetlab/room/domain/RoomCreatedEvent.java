package com.example.meetlab.room.domain;

import java.time.Instant;

/** 방이 만들어졌음을 알리는 이벤트. 검색 색인처럼 방 도메인 밖의 후속 작업이 구독한다. */
public record RoomCreatedEvent(String code, String name, int maxParticipants, Instant createdAt) {

    public static RoomCreatedEvent from(Room room) {
        return new RoomCreatedEvent(room.getCode(), room.getName(), room.getMaxParticipants(), room.getCreatedAt());
    }
}
