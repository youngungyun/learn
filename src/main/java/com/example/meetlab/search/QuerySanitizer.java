package com.example.meetlab.search;

/**
 * 검색어를 JSON 질의에 넣기 전에 정리한다.
 *
 * <p>검색어는 JSON 문자열 안에 들어가므로 따옴표와 역슬래시, 제어 문자가 있으면 질의 구조가 깨지거나
 * 의도하지 않은 질의가 만들어질 수 있다(인젝션). 해당 문자를 공백으로 바꾸고 길이를 제한한다.
 */
final class QuerySanitizer {

    static final int MAX_LENGTH = 50;

    private QuerySanitizer() {
    }

    /** 정리한 검색어. 비어 있으면 검색하지 않아야 한다는 뜻으로 빈 문자열을 돌려준다. */
    static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.replaceAll("[\"\\\\\\p{Cntrl}]", " ").replaceAll("\\s+", " ").strip();
        return cleaned.length() > MAX_LENGTH ? cleaned.substring(0, MAX_LENGTH).strip() : cleaned;
    }
}
