package com.example.meetlab.telemetry;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/telemetry")
public class WebRtcTelemetryController {

    private final WebRtcTelemetry telemetry;

    public WebRtcTelemetryController(WebRtcTelemetry telemetry) {
        this.telemetry = telemetry;
    }

    /** 처리 결과를 돌려줄 필요가 없는 "던지고 잊는" 보고이므로 202 로 답한다. */
    @PostMapping("/webrtc")
    public ResponseEntity<Void> report(@Valid @RequestBody WebRtcStatsRequest request) {
        telemetry.record(request);
        return ResponseEntity.accepted().build();
    }
}
