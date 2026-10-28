package com.example.meetlab.signaling;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 방별 접속자 목록을 메모리에 들고 있다.
 *
 * <p>"정원 확인 후 추가"가 한 덩어리로 일어나야 정원 초과 입장을 막을 수 있어서
 * 모든 변경 메서드를 하나의 락(synchronized)으로 보호한다. 학습용 단일 서버 전제이며,
 * 서버를 여러 대로 늘리면 Redis 등 공유 저장소가 필요하다.
 */
@Component
public class RoomSessionRegistry {

    /** 접속자 한 명의 상태. */
    public record Member(PeerConnection connection, String displayName) {
        PeerInfo info() {
            return new PeerInfo(connection.id(), displayName);
        }
    }

    /** 입장 결과. 입장 전에 방에 있던 사람들 목록을 함께 돌려준다. */
    public record JoinResult(List<Member> existing) {
    }

    private final Map<String, Map<String, Member>> rooms = new LinkedHashMap<>();
    private final Map<String, String> roomByPeer = new LinkedHashMap<>();

    /** 정원이 남아 있고 아직 어느 방에도 없을 때만 입장시킨다. 실패하면 empty. */
    public synchronized Optional<JoinResult> join(String roomCode, int capacity, Member member) {
        String peerId = member.connection().id();
        if (roomByPeer.containsKey(peerId)) {
            return Optional.empty();
        }
        Map<String, Member> members = rooms.computeIfAbsent(roomCode, key -> new LinkedHashMap<>());
        if (members.size() >= capacity) {
            return Optional.empty();
        }
        List<Member> existing = new ArrayList<>(members.values());
        members.put(peerId, member);
        roomByPeer.put(peerId, roomCode);
        return Optional.of(new JoinResult(existing));
    }

    /** 방에서 내보내고, 그 방에 남은 사람들을 돌려준다. 방에 없던 연결이면 empty. */
    public synchronized Optional<List<Member>> leave(String peerId) {
        String roomCode = roomByPeer.remove(peerId);
        if (roomCode == null) {
            return Optional.empty();
        }
        Map<String, Member> members = rooms.get(roomCode);
        members.remove(peerId);
        if (members.isEmpty()) {
            rooms.remove(roomCode);
            return Optional.of(List.of());
        }
        return Optional.of(new ArrayList<>(members.values()));
    }

    /** 보낸 사람과 받는 사람이 같은 방에 있을 때만 받는 사람의 연결을 돌려준다. */
    public synchronized Optional<PeerConnection> findInSameRoom(String fromPeerId, String toPeerId) {
        String roomCode = roomByPeer.get(fromPeerId);
        if (roomCode == null || !roomCode.equals(roomByPeer.get(toPeerId))) {
            return Optional.empty();
        }
        return Optional.of(rooms.get(roomCode).get(toPeerId).connection());
    }

    public synchronized int sizeOf(String roomCode) {
        Map<String, Member> members = rooms.get(roomCode);
        return members == null ? 0 : members.size();
    }

    public synchronized int totalPeers() {
        return roomByPeer.size();
    }
}
