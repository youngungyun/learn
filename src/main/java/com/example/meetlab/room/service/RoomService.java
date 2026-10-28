package com.example.meetlab.room.service;

import com.example.meetlab.room.domain.Room;
import com.example.meetlab.room.domain.RoomCodeGenerator;
import com.example.meetlab.room.domain.RoomCreatedEvent;
import com.example.meetlab.room.domain.RoomRepository;
import com.example.meetlab.room.exception.RoomNotFoundException;
import java.time.Clock;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoomService {

    static final int MAX_CODE_ATTEMPTS = 5;

    private final RoomRepository roomRepository;
    private final RoomCodeGenerator codeGenerator;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    public RoomService(RoomRepository roomRepository, RoomCodeGenerator codeGenerator, Clock clock,
            ApplicationEventPublisher eventPublisher) {
        this.roomRepository = roomRepository;
        this.codeGenerator = codeGenerator;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 방을 만든다. 최대 인원을 생략하면 허용되는 최대값(4명)을 쓴다.
     */
    @Transactional
    public Room create(String name, Integer maxParticipants) {
        int capacity = maxParticipants == null ? Room.MAX_PARTICIPANTS : maxParticipants;
        String code = nextAvailableCode();
        Room saved = roomRepository.save(new Room(code, name, capacity, clock.instant()));
        // 검색 색인 등 "방이 생긴 뒤의 일"은 이벤트로 알린다. 방 도메인이 검색을 알 필요가 없다.
        eventPublisher.publishEvent(RoomCreatedEvent.from(saved));
        return saved;
    }

    public Room get(String code) {
        return roomRepository.findById(code).orElseThrow(() -> new RoomNotFoundException(code));
    }

    public List<Room> recent() {
        return roomRepository.findTop10ByOrderByCreatedAtDesc();
    }

    private String nextAvailableCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String candidate = codeGenerator.generate();
            if (!roomRepository.existsById(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("사용 가능한 방 코드를 만들지 못했습니다.");
    }
}
