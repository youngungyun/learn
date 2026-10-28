package com.example.meetlab.room.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Room")
class RoomTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("올바른 값으로 만들 수 있고 이름의 앞뒤 공백은 제거된다")
        void createsWithValidValuesAndTrimsName() {
            Room room = new Room("abc-defg-hij", "  주간 회의  ", 3, NOW);

            assertThat(room.getCode()).isEqualTo("abc-defg-hij");
            assertThat(room.getName()).isEqualTo("주간 회의");
            assertThat(room.getMaxParticipants()).isEqualTo(3);
            assertThat(room.getCreatedAt()).isEqualTo(NOW);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("코드가 비어 있으면 만들 수 없다")
        void rejectsBlankCode(String code) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new Room(code, "회의", 2, NOW))
                    .withMessageContaining("코드");
        }

        @Test
        @DisplayName("코드가 null이면 만들 수 없다")
        void rejectsNullCode() {
            assertThatIllegalArgumentException().isThrownBy(() -> new Room(null, "회의", 2, NOW));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("이름이 비어 있으면 만들 수 없다")
        void rejectsBlankName(String name) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new Room("abc-defg-hij", name, 2, NOW))
                    .withMessageContaining("이름");
        }

        @Test
        @DisplayName("이름이 null이면 만들 수 없다")
        void rejectsNullName() {
            assertThatIllegalArgumentException().isThrownBy(() -> new Room("abc-defg-hij", null, 2, NOW));
        }

        @Test
        @DisplayName("이름은 50자까지 허용하고 51자는 거부한다(경계값)")
        void enforcesNameLengthBoundary() {
            String fifty = "가".repeat(Room.MAX_NAME_LENGTH);
            String fiftyOne = "가".repeat(Room.MAX_NAME_LENGTH + 1);

            assertThat(new Room("abc-defg-hij", fifty, 2, NOW).getName()).hasSize(50);
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new Room("abc-defg-hij", fiftyOne, 2, NOW))
                    .withMessageContaining("50자");
        }

        @ParameterizedTest(name = "최대 인원 {0}명이면 허용={1}")
        @CsvSource({"1, false", "2, true", "3, true", "4, true", "5, false"})
        @DisplayName("최대 인원은 2~4명만 허용한다(경계값)")
        void enforcesParticipantBoundary(int maxParticipants, boolean allowed) {
            if (allowed) {
                assertThat(new Room("abc-defg-hij", "회의", maxParticipants, NOW).getMaxParticipants())
                        .isEqualTo(maxParticipants);
            } else {
                assertThatIllegalArgumentException()
                        .isThrownBy(() -> new Room("abc-defg-hij", "회의", maxParticipants, NOW))
                        .withMessageContaining("최대 인원");
            }
        }

        @Test
        @DisplayName("생성 시각이 null이면 NullPointerException")
        void rejectsNullCreatedAt() {
            assertThatNullPointerException().isThrownBy(() -> new Room("abc-defg-hij", "회의", 2, null));
        }
    }

    @Nested
    @DisplayName("정원 확인")
    class Capacity {

        @ParameterizedTest(name = "정원 3명 방에 {0}명이 있으면 입장 가능={1}")
        @CsvSource({"0, true", "2, true", "3, false", "4, false"})
        void hasCapacityOnlyBelowMax(int current, boolean expected) {
            Room room = new Room("abc-defg-hij", "회의", 3, NOW);

            assertThat(room.hasCapacityFor(current)).isEqualTo(expected);
        }
    }
}
