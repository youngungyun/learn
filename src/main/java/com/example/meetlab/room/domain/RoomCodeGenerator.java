package com.example.meetlab.room.domain;

import java.util.regex.Pattern;
import java.util.random.RandomGenerator;

/**
 * 초대 링크에 쓰는 방 코드 생성기. 형식은 {@code abc-defg-hij}.
 *
 * <p>헷갈리기 쉬운 글자(i, l, o)는 뺀다. 난수 공급원을 주입받아 테스트에서 결과를 고정할 수 있다.
 */
public class RoomCodeGenerator {

    private static final String ALPHABET = "abcdefghjkmnpqrstuvwxyz";
    private static final Pattern WELL_FORMED = Pattern.compile("^[a-z]{3}-[a-z]{4}-[a-z]{3}$");

    private final RandomGenerator random;

    public RoomCodeGenerator(RandomGenerator random) {
        this.random = random;
    }

    public String generate() {
        return segment(3) + "-" + segment(4) + "-" + segment(3);
    }

    /** 외부에서 들어온 코드가 형식에 맞는지 가볍게 확인한다. 존재 여부는 확인하지 않는다. */
    public static boolean isWellFormed(String code) {
        return code != null && WELL_FORMED.matcher(code).matches();
    }

    private String segment(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
