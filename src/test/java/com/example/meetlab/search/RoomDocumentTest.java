package com.example.meetlab.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.meetlab.room.domain.Room;
import com.example.meetlab.room.domain.RoomCreatedEvent;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RoomDocumentTest {

    private static final Instant CREATED = Instant.parse("2026-10-29T01:02:03.456Z");

    @Test
    void convertsFromRoom() {
        RoomDocument document = RoomDocument.from(new Room("abc-defg-hij", "주간 회의", 3, CREATED));

        assertThat(document.code()).isEqualTo("abc-defg-hij");
        assertThat(document.name()).isEqualTo("주간 회의");
        assertThat(document.maxParticipants()).isEqualTo(3);
        assertThat(document.createdAt()).isEqualTo(CREATED.toEpochMilli());
    }

    @Test
    void convertsFromEvent() {
        RoomDocument document = RoomDocument.from(new RoomCreatedEvent("abc-defg-hij", "회의", 2, CREATED));

        assertThat(document.createdAt()).isEqualTo(1_793_235_723_456L);
    }
}
