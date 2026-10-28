package com.example.meetlab.signaling;

/**
 * 시그널링 서비스가 바라보는 "연결 한 개". WebSocket 에 의존하지 않게 인터페이스로 분리해 두면
 * 서비스 로직을 가짜 연결로 빠르게 테스트할 수 있다.
 */
public interface PeerConnection {

    String id();

    void send(ServerMessage message);
}
