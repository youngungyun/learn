package com.example.meetlab.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.meetlab.room.domain.Room;
import com.example.meetlab.room.domain.RoomRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class RoomReindexServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-29T00:00:00Z");

    private static List<Room> rooms(int from, int count) {
        List<Room> rooms = new ArrayList<>();
        for (int i = from; i < from + count; i++) {
            rooms.add(new Room(String.format("code-%04d", i), "방 " + i, 2, NOW));
        }
        return rooms;
    }

    @Test
    void indexesEveryPageUntilExhausted() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomSearchRepository searchRepository = mock(RoomSearchRepository.class);
        int batch = RoomReindexService.BATCH_SIZE;
        long total = batch + 3L;
        given(roomRepository.findAll(any(Pageable.class))).willAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(0);
            int from = (int) pageable.getOffset();
            int count = (int) Math.min(batch, total - from);
            PageRequest request = PageRequest.of(pageable.getPageNumber(), batch);
            return (Page<Room>) new PageImpl<>(rooms(from, count), request, total);
        });

        long indexed = new RoomReindexService(roomRepository, searchRepository).reindexAll();

        assertThat(indexed).isEqualTo(total);
        verify(searchRepository, times(2)).saveAll(any(Iterable.class));
    }

    @Test
    void emptyDatabaseIndexesNothing() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomSearchRepository searchRepository = mock(RoomSearchRepository.class);
        given(roomRepository.findAll(any(Pageable.class))).willReturn(Page.empty());

        long indexed = new RoomReindexService(roomRepository, searchRepository).reindexAll();

        assertThat(indexed).isZero();
    }
}
