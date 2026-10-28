package com.example.meetlab.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QuerySanitizerTest {

    @Test
    void nullAndBlankBecomeEmpty() {
        assertThat(QuerySanitizer.sanitize(null)).isEmpty();
        assertThat(QuerySanitizer.sanitize("   \t\n ")).isEmpty();
    }

    @Test
    void keepsKoreanAndCollapsesWhitespace() {
        assertThat(QuerySanitizer.sanitize("  주간   회의 ")).isEqualTo("주간 회의");
    }

    @Test
    void removesCharactersThatCouldBreakJsonQuery() {
        assertThat(QuerySanitizer.sanitize("a\"}},{\"match_all\":{}}")).doesNotContain("\"");
        assertThat(QuerySanitizer.sanitize("back\\slash")).isEqualTo("back slash");
        assertThat(QuerySanitizer.sanitize("line1\nline2\u0000")).isEqualTo("line1 line2");
    }

    @Test
    void truncatesToMaxLength() {
        String result = QuerySanitizer.sanitize("가".repeat(200));

        assertThat(result).hasSize(QuerySanitizer.MAX_LENGTH);
    }
}
