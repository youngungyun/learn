package com.example.meetlab.signaling;

/** 클라이언트와 서버가 주고받는 메시지 유형. 브라우저의 app.js 와 같은 문자열을 쓴다. */
public final class SignalingTypes {

    // 클라이언트 -> 서버
    public static final String JOIN = "join";
    public static final String LEAVE = "leave";
    public static final String OFFER = "offer";
    public static final String ANSWER = "answer";
    public static final String CANDIDATE = "candidate";

    // 서버 -> 클라이언트
    public static final String JOINED = "joined";
    public static final String PEER_JOINED = "peer-joined";
    public static final String PEER_LEFT = "peer-left";
    public static final String ERROR = "error";

    private SignalingTypes() {
    }
}
