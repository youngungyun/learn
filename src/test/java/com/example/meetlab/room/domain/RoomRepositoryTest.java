package com.example.meetlab.room.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/**
 * JPA 계층만 올리는 슬라이스 테스트. 임베디드 H2가 자동으로 연결되고 각 테스트는 롤백된다.
 */
@DataJpaTest
@DisplayName("RoomRepository")
class RoomRepositoryTest {

    @Autowired
    private RoomRepository roomRepository;

    @Test
    @DisplayName("방 코드로 저장하고 조회한다")
    void savesAndFindsByCode() {
        roomRepository.save(new Room("abc-defg-hij", "주간 회의", 4, Instant.parse("2026-01-01T00:00:00Z")));

        assertThat(roomRepository.findById("abc-defg-hij"))
                .hasValueSatisfying(room -> {
                    assertThat(room.getName()).isEqualTo("주간 회의");
                    assertThat(room.getMaxParticipants()).isEqualTo(4);
                });
    }

    @Test
    @DisplayName("없는 코드는 비어 있는 Optional을 돌려준다")
    void returnsEmptyForUnknownCode() {
        assertThat(roomRepository.findById("zzz-zzzz-zzz")).isEmpty();
    }

    @Test
    @DisplayName("최근 방 목록은 최신순으로 최대 10개만 가져온다")
    void findsTenMostRecentRoomsNewestFirst() {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        for (int i = 0; i < 12; i++) {
            String code = "aaa-aaaa-" + (char) ('a' + i) + "aa";
            roomRepository.save(new Room(code, "방 " + i, 2, base.plusSeconds(i)));
        }

        assertThat(roomRepository.findTop10ByOrderByCreatedAtDesc())
                .hasSize(10)
                .extracting(Room::getName)
                .startsWith("방 11", "방 10")
                .endsWith("방 2");
    }
}
