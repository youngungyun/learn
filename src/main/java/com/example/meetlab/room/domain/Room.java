package com.example.meetlab.room.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * 화상 통화 방. 방 코드(code)가 곧 식별자이며 초대 링크에 그대로 쓰인다.
 *
 * <p>Mesh 방식 WebRTC는 참가자가 늘수록 연결 수가 N(N-1)/2로 늘어나므로 최대 인원을 4명으로 제한한다.
 */
@Entity
@Table(name = "rooms")
public class Room {

    public static final int MIN_PARTICIPANTS = 2;
    public static final int MAX_PARTICIPANTS = 4;
    public static final int MAX_NAME_LENGTH = 50;

    @Id
    @Column(length = 20, nullable = false, updatable = false)
    private String code;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(nullable = false)
    private int maxParticipants;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Room() {
        // JPA 전용 기본 생성자
    }

    public Room(String code, String name, int maxParticipants, Instant createdAt) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("방 코드는 비어 있을 수 없습니다.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("방 이름은 비어 있을 수 없습니다.");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("방 이름은 " + MAX_NAME_LENGTH + "자 이하여야 합니다.");
        }
        if (maxParticipants < MIN_PARTICIPANTS || maxParticipants > MAX_PARTICIPANTS) {
            throw new IllegalArgumentException(
                    "최대 인원은 " + MIN_PARTICIPANTS + "~" + MAX_PARTICIPANTS + "명이어야 합니다: " + maxParticipants);
        }
        this.code = code;
        this.name = trimmed;
        this.maxParticipants = maxParticipants;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /** 현재 참가자 수가 이미 정원이면 false. */
    public boolean hasCapacityFor(int currentParticipants) {
        return currentParticipants < maxParticipants;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
