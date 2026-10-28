package com.example.meetlab.signaling;

import com.example.meetlab.room.domain.Room;
import com.example.meetlab.room.exception.RoomNotFoundException;
import com.example.meetlab.room.service.RoomService;
import com.example.meetlab.signaling.RoomSessionRegistry.JoinResult;
import com.example.meetlab.signaling.RoomSessionRegistry.Member;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * WebRTC 시그널링의 핵심 로직. 서버는 미디어를 다루지 않고 입장/퇴장 알림과
 * SDP(offer/answer), ICE candidate 를 상대에게 전달만 한다.
 *
 * <p>규칙: 새로 들어온 사람이 기존 참가자 모두에게 offer 를 보낸다.
 * 그래서 입장 응답(joined)에 기존 참가자 목록을 담고, 기존 참가자에게는 peer-joined 만 알린다.
 */
@Service
public class SignalingService {

    private static final Logger LOG = LoggerFactory.getLogger(SignalingService.class);
    static final int MAX_DISPLAY_NAME_LENGTH = 30;

    private final RoomService roomService;
    private final RoomSessionRegistry registry;
    private final SignalingMetrics metrics;

    public SignalingService(RoomService roomService, RoomSessionRegistry registry, SignalingMetrics metrics) {
        this.roomService = roomService;
        this.registry = registry;
        this.metrics = metrics;
    }

    public void handle(PeerConnection from, ClientMessage message) {
        if (message == null || message.type() == null) {
            from.send(ServerMessage.error("BAD_REQUEST", "type 이 필요합니다."));
            return;
        }
        metrics.record(message.type(), () -> dispatch(from, message));
    }

    private void dispatch(PeerConnection from, ClientMessage message) {
        switch (message.type()) {
            case SignalingTypes.JOIN -> join(from, message);
            case SignalingTypes.LEAVE -> disconnected(from);
            case SignalingTypes.OFFER, SignalingTypes.ANSWER, SignalingTypes.CANDIDATE -> relay(from, message);
            default -> from.send(ServerMessage.error("UNKNOWN_TYPE", "알 수 없는 메시지 유형입니다: " + message.type()));
        }
    }

    /** 연결이 끊기거나 leave 를 받았을 때 호출한다. 여러 번 불려도 안전하다. */
    public void disconnected(PeerConnection connection) {
        Optional<List<Member>> remaining = registry.leave(connection.id());
        remaining.ifPresent(members -> {
            ServerMessage left = ServerMessage.peerLeft(connection.id());
            members.forEach(member -> member.connection().send(left));
            LOG.atInfo()
                    .addKeyValue("event", "peer_left")
                    .addKeyValue("peer_id", connection.id())
                    .addKeyValue("remaining_peers", members.size())
                    .log("퇴장");
        });
    }

    private void join(PeerConnection from, ClientMessage message) {
        Room room;
        try {
            room = roomService.get(message.room());
        } catch (RoomNotFoundException e) {
            metrics.joinRejected("room_not_found");
            from.send(ServerMessage.error("ROOM_NOT_FOUND", "존재하지 않는 방입니다."));
            return;
        }

        Member member = new Member(from, sanitize(message.displayName(), from.id()));
        Optional<JoinResult> result = registry.join(room.getCode(), room.getMaxParticipants(), member);
        if (result.isEmpty()) {
            metrics.joinRejected("full_or_duplicate");
            LOG.atWarn()
                    .addKeyValue("event", "join_rejected")
                    .addKeyValue("room", room.getCode())
                    .addKeyValue("peer_id", from.id())
                    .log("입장 거절(정원 초과 또는 중복 입장)");
            from.send(ServerMessage.error("JOIN_REJECTED", "방이 가득 찼거나 이미 입장한 상태입니다."));
            return;
        }

        metrics.joinSucceeded();
        List<PeerInfo> existing = result.get().existing().stream().map(Member::info).toList();
        from.send(ServerMessage.joined(from.id(), existing));
        ServerMessage joined = ServerMessage.peerJoined(member.info());
        result.get().existing().forEach(other -> other.connection().send(joined));
        LOG.atInfo()
                .addKeyValue("event", "peer_joined")
                .addKeyValue("room", room.getCode())
                .addKeyValue("peer_id", from.id())
                .addKeyValue("existing_peers", existing.size())
                .log("입장");
    }

    private void relay(PeerConnection from, ClientMessage message) {
        if (message.to() == null || message.data() == null) {
            from.send(ServerMessage.error("BAD_REQUEST", "to 와 data 가 필요합니다."));
            return;
        }
        Optional<PeerConnection> target = registry.findInSameRoom(from.id(), message.to());
        if (target.isEmpty()) {
            from.send(ServerMessage.error("PEER_NOT_FOUND", "같은 방에 없는 상대입니다."));
            return;
        }
        target.get().send(ServerMessage.relayed(message.type(), from.id(), message.data()));
    }

    private static String sanitize(String displayName, String fallback) {
        if (displayName == null || displayName.isBlank()) {
            return "guest-" + fallback.substring(0, Math.min(4, fallback.length()));
        }
        String trimmed = displayName.strip();
        return trimmed.length() > MAX_DISPLAY_NAME_LENGTH ? trimmed.substring(0, MAX_DISPLAY_NAME_LENGTH) : trimmed;
    }
}
