package com.example.meetlab.ice;

import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ice-servers")
public class IceServerController {

    private final IceServerService iceServerService;

    public IceServerController(IceServerService iceServerService) {
        this.iceServerService = iceServerService;
    }

    /**
     * 임시 TURN 자격 증명이 들어 있으므로 브라우저나 중간 프록시가 캐시하지 않게 한다.
     * 로그인 개념이 없는 학습용이라 사용자 ID 는 요청마다 새로 만든다.
     */
    @GetMapping
    public ResponseEntity<IceServersResponse> iceServers() {
        IceServersResponse body = iceServerService.issue(UUID.randomUUID().toString());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
