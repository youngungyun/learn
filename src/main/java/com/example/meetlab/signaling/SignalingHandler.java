package com.example.meetlab.signaling;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

/** WebSocket 이벤트를 SignalingService 호출로 바꿔 주는 얇은 어댑터. */
public class SignalingHandler extends TextWebSocketHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SignalingHandler.class);
    private static final String MDC_PEER_ID = "peer_id";

    private final SignalingService signalingService;
    private final ObjectMapper objectMapper;
    private final Map<String, PeerConnection> connections = new ConcurrentHashMap<>();

    public SignalingHandler(SignalingService signalingService, ObjectMapper objectMapper) {
        this.signalingService = signalingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        connections.put(session.getId(), new WebSocketPeerConnection(session, objectMapper));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
        PeerConnection connection = connections.get(session.getId());
        if (connection == null) {
            return;
        }
        // WebSocket 에는 HTTP 요청 필터가 없으므로 메시지를 처리하는 동안 peer_id 를 MDC 에 직접 넣는다.
        MDC.put(MDC_PEER_ID, session.getId());
        try {
            ClientMessage message;
            try {
                message = objectMapper.readValue(textMessage.getPayload(), ClientMessage.class);
            } catch (RuntimeException e) {
                connection.send(ServerMessage.error("BAD_REQUEST", "JSON 형식이 올바르지 않습니다."));
                return;
            }
            try {
                signalingService.handle(connection, message);
            } catch (RuntimeException e) {
                LOG.error("시그널링 처리 중 오류", e);
                connection.send(ServerMessage.error("INTERNAL", "서버 오류가 발생했습니다."));
            }
        } finally {
            MDC.remove(MDC_PEER_ID);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        PeerConnection connection = connections.remove(session.getId());
        if (connection == null) {
            return;
        }
        MDC.put(MDC_PEER_ID, session.getId());
        try {
            signalingService.disconnected(connection);
        } finally {
            MDC.remove(MDC_PEER_ID);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        LOG.warn("전송 오류: peerId={}, cause={}", session.getId(), exception.toString());
    }
}
