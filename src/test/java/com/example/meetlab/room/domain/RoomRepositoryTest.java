package com.example.meetlab.room.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.meetlab.TestcontainersConfiguration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * JPA 계층만 올리는 슬라이스 테스트. 임베디드 DB 로 바꿔치기하지 않고(replace=none)
 * Testcontainers 의 진짜 PostgreSQL 에 붙으며, Flyway 마이그레이션이 적용된 스키마를 검증한다.
 * 각 테스트는 롤백된다.
 */
@DataJpaTest(properties = "spring.test.database.replace=none")
@Import(TestcontainersConfiguration.class)
@DisplayName("RoomRepository (PostgreSQL)")
class RoomRepositoryTest {

    private static final String INSERT_ROOM =
            "INSERT INTO rooms (code, name, max_participants, created_at) VALUES (?, ?, ?, now())";

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("진짜 PostgreSQL 18 에 연결되어 있다")
    void runsOnRealPostgres() {
        String version = jdbcTemplate.queryForObject("SELECT version()", String.class);

        assertThat(version).startsWith("PostgreSQL 18");
    }

    @Test
    @DisplayName("같은 방 코드를 두 번 넣으면 DB 가 거부한다")
    void rejectsDuplicateCode() {
        // JPA save() 는 식별자가 있으면 merge(UPDATE)로 동작해 중복을 못 잡는다. 그래서 SQL 로 직접 넣는다.
        jdbcTemplate.update(INSERT_ROOM, "dup-dupp-dup", "첫째", 2);

        assertThatThrownBy(() -> jdbcTemplate.update(INSERT_ROOM, "dup-dupp-dup", "둘째", 2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("최대 인원이 2~4 를 벗어나면 CHECK 제약이 거부한다")
    void rejectsOutOfRangeCapacity() {
        assertThatThrownBy(() -> jdbcTemplate.update(INSERT_ROOM, "bad-badd-bad", "범위 밖", 5))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

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
