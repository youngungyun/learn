package com.example.meetlab.signaling;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import tools.jackson.databind.ObjectMapper;

/**
 * WebSocketSession 을 PeerConnection 으로 감싼다.
 *
 * <p>WebSocketSession 은 여러 스레드가 동시에 sendMessage 를 호출하면 안 된다.
 * 다른 참가자의 요청을 처리하는 스레드가 이 연결로 메시지를 보내기 때문에
 * ConcurrentWebSocketSessionDecorator 로 감싸 전송을 직렬화한다.
 */
public class WebSocketPeerConnection implements PeerConnection {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketPeerConnection.class);
    private static final int SEND_TIME_LIMIT_MILLIS = 5_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 512 * 1024;

    private final String id;
    private final WebSocketSession session;
    private final ObjectMapper objectMapper;

    public WebSocketPeerConnection(WebSocketSession session, ObjectMapper objectMapper) {
        this.id = session.getId();
        this.session = new ConcurrentWebSocketSessionDecorator(
                session, SEND_TIME_LIMIT_MILLIS, BUFFER_SIZE_LIMIT_BYTES);
        this.objectMapper = objectMapper;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void send(ServerMessage message) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (IOException | RuntimeException e) {
            // 한 참가자에게 보내지 못했다고 다른 참가자에 대한 처리까지 막으면 안 된다.
            LOG.warn("메시지 전송 실패: peerId={}, type={}, cause={}", id, message.type(), e.toString());
        }
    }
}
