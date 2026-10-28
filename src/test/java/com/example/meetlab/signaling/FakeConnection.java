package com.example.meetlab.signaling;

import java.util.ArrayList;
import java.util.List;

/** 테스트용 연결. 받은 메시지를 모아 둔다. */
class FakeConnection implements PeerConnection {

    private final String id;
    private final List<ServerMessage> received = new ArrayList<>();

    FakeConnection(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void send(ServerMessage message) {
        received.add(message);
    }

    List<ServerMessage> received() {
        return received;
    }

    ServerMessage last() {
        return received.get(received.size() - 1);
    }
}
