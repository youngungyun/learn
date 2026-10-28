package com.example.meetlab.ice;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * 브라우저 RTCPeerConnection 의 iceServers 항목과 같은 모양이다.
 * STUN 은 인증이 없으므로 username, credential 이 JSON 에서 빠진다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record IceServer(List<String> urls, String username, String credential) {
}
