package com.example.meetlab.signaling;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * 서버가 브라우저로 보내는 메시지. 값이 없는 필드는 JSON 에서 빠진다.
 *
 * @param type 메시지 유형
 * @param id joined 일 때는 내 peer id, peer-joined/peer-left 일 때는 상대 peer id
 * @param from offer/answer/candidate 를 보낸 peer id (서버가 채운다)
 * @param displayName peer-joined 일 때의 표시 이름
 * @param peers joined 일 때 이미 방에 있던 사람들
 * @param data 그대로 전달하는 SDP 또는 ICE candidate
 * @param code error 일 때의 오류 코드
 * @param message error 일 때의 설명
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServerMessage(
        String type,
        String id,
        String from,
        String displayName,
        List<PeerInfo> peers,
        JsonNode data,
        String code,
        String message) {

    public static ServerMessage joined(String myId, List<PeerInfo> peers) {
        return new ServerMessage(SignalingTypes.JOINED, myId, null, null, peers, null, null, null);
    }

    public static ServerMessage peerJoined(PeerInfo peer) {
        return new ServerMessage(SignalingTypes.PEER_JOINED, peer.id(), null, peer.displayName(),
                null, null, null, null);
    }

    public static ServerMessage peerLeft(String peerId) {
        return new ServerMessage(SignalingTypes.PEER_LEFT, peerId, null, null, null, null, null, null);
    }

    public static ServerMessage relayed(String type, String from, JsonNode data) {
        return new ServerMessage(type, null, from, null, null, data, null, null);
    }

    public static ServerMessage error(String code, String message) {
        return new ServerMessage(SignalingTypes.ERROR, null, null, null, null, null, code, message);
    }
}
