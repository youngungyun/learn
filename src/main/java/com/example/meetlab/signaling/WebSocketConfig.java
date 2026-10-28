package com.example.meetlab.signaling;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    static final String PATH = "/ws/signaling";
    private static final int MAX_TEXT_MESSAGE_BYTES = 64 * 1024;

    private final SignalingService signalingService;
    private final ObjectMapper objectMapper;

    public WebSocketConfig(SignalingService signalingService, ObjectMapper objectMapper) {
        this.signalingService = signalingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 학습용으로 같은 출처(localhost)만 허용한다. 배포 도메인이 생기면 여기에 추가한다.
        registry.addHandler(new SignalingHandler(signalingService, objectMapper), PATH)
                .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*");
    }

    /** SDP 는 수 KB 이므로 한 메시지를 64KB 로 제한해 비정상적으로 큰 메시지를 막는다. */
    @Bean
    public ServletServerContainerFactoryBean webSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(MAX_TEXT_MESSAGE_BYTES);
        return container;
    }
}
