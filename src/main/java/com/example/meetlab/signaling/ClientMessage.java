package com.example.meetlab.signaling;

import tools.jackson.databind.JsonNode;

/**
 * 브라우저가 서버로 보내는 메시지.
 *
 * <p>보낸 사람(from)은 일부러 필드로 두지 않았다. 서버가 연결 정보로 직접 채워야 위조할 수 없다.
 *
 * @param type 메시지 유형({@link SignalingTypes})
 * @param room join 일 때의 방 코드
 * @param displayName join 일 때의 표시 이름(생략 가능)
 * @param to offer/answer/candidate 를 받을 상대의 peer id
 * @param data SDP 또는 ICE candidate. 서버는 내용을 해석하지 않고 그대로 전달한다.
 */
public record ClientMessage(String type, String room, String displayName, String to, JsonNode data) {
}
