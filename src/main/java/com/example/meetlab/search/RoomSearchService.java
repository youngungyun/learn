package com.example.meetlab.search;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class RoomSearchService {

    static final int DEFAULT_SIZE = 10;
    static final int MAX_SIZE = 20;

    private final RoomSearchRepository repository;
    private final MeterRegistry meterRegistry;

    public RoomSearchService(RoomSearchRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
    }

    public List<RoomSearchResult> search(String rawQuery, Integer requestedSize) {
        String text = QuerySanitizer.sanitize(rawQuery);
        if (text.isEmpty()) {
            return List.of();
        }
        int size = requestedSize == null ? DEFAULT_SIZE : Math.max(1, Math.min(requestedSize, MAX_SIZE));
        return Timer.builder("meetlab.search.query")
                .description("방 검색 질의 시간")
                .register(meterRegistry)
                .record(() -> repository.search(text, PageRequest.of(0, size)).stream()
                        .map(hit -> new RoomSearchResult(hit.getContent().code(), hit.getContent().name(),
                                hit.getContent().maxParticipants(), hit.getScore()))
                        .toList());
    }
}
