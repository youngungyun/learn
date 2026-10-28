package com.example.meetlab.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.meetlab.room.domain.RoomCreatedEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoomIndexerTest {

    private static final RoomCreatedEvent EVENT =
            new RoomCreatedEvent("abc-defg-hij", "주간 회의", 4, Instant.parse("2026-10-29T00:00:00Z"));

    private RoomSearchRepository repository;
    private SimpleMeterRegistry meters;
    private RoomIndexer indexer;

    @BeforeEach
    void setUp() {
        repository = mock(RoomSearchRepository.class);
        meters = new SimpleMeterRegistry();
        indexer = new RoomIndexer(repository, meters);
    }

    @Test
    void indexesDocumentAndCountsSuccess() {
        indexer.onRoomCreated(EVENT);

        verify(repository).save(RoomDocument.from(EVENT));
        assertThat(meters.get("meetlab.search.index").tag("result", "success").counter().count()).isEqualTo(1.0);
    }

    @Test
    void swallowsIndexingFailureSoRoomCreationIsNotAffected() {
        given(repository.save(any(RoomDocument.class))).willThrow(new IllegalStateException("ES down"));

        indexer.onRoomCreated(EVENT); // 예외가 밖으로 나오면 안 된다

        assertThat(meters.get("meetlab.search.index").tag("result", "failure").counter().count()).isEqualTo(1.0);
    }
}
