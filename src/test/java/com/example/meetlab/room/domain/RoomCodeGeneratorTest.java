package com.example.meetlab.room.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

@DisplayName("RoomCodeGenerator")
class RoomCodeGeneratorTest {

    @Test
    @DisplayName("생성된 코드는 항상 abc-defg-hij 형식이고 헷갈리는 글자(i, l, o)를 쓰지 않는다")
    void generatesWellFormedCodesWithoutAmbiguousLetters() {
        RoomCodeGenerator generator = new RoomCodeGenerator(new Random(42));

        for (int i = 0; i < 1_000; i++) {
            String code = generator.generate();

            assertThat(RoomCodeGenerator.isWellFormed(code)).isTrue();
            assertThat(code.replace("-", "")).doesNotContain("i", "l", "o");
        }
    }

    @Test
    @DisplayName("같은 시드를 쓰면 같은 코드가 나온다(테스트에서 결과를 고정할 수 있다)")
    void isDeterministicForTheSameSeed() {
        RoomCodeGenerator first = new RoomCodeGenerator(new Random(7));
        RoomCodeGenerator second = new RoomCodeGenerator(new Random(7));

        assertThat(first.generate()).isEqualTo(second.generate());
    }

    @Test
    @DisplayName("연속 생성한 코드는 거의 겹치지 않는다")
    void rarelyCollides() {
        RoomCodeGenerator generator = new RoomCodeGenerator(new Random(1));
        Set<String> codes = new HashSet<>();

        for (int i = 0; i < 1_000; i++) {
            codes.add(generator.generate());
        }

        assertThat(codes).hasSize(1_000);
    }

    @ParameterizedTest(name = "\"{0}\" 형식 검사 결과는 {1}")
    @CsvSource({
            "abc-defg-hij, true",
            "ABC-DEFG-HIJ, false",
            "abc-defg-hi, false",
            "abcdefghij, false",
            "abc-defg-hij-k, false",
            "abc-def1-hij, false"
    })
    void checksFormat(String code, boolean expected) {
        assertThat(RoomCodeGenerator.isWellFormed(code)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("null이나 빈 문자열은 형식에 맞지 않는다")
    void rejectsNullAndEmpty(String code) {
        assertThat(RoomCodeGenerator.isWellFormed(code)).isFalse();
    }
}
