package com.example.meetlab.telemetry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WebRtcTelemetryController.class)
class WebRtcTelemetryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WebRtcTelemetry telemetry;

    private void postJson(String json, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/telemetry/webrtc").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void acceptsValidReport() throws Exception {
        postJson("{\"candidateType\":\"srflx\",\"rttMs\":40,\"packetLossRatio\":0.01,\"jitterMs\":4}", 202);

        verify(telemetry).record(any(WebRtcStatsRequest.class));
    }

    @Test
    void rejectsUnknownCandidateTypeBecauseItBecomesAMetricTag() throws Exception {
        postJson("{\"candidateType\":\"my-own-label\",\"rttMs\":40,\"packetLossRatio\":0.01,\"jitterMs\":4}", 400);

        verify(telemetry, never()).record(any());
    }

    @Test
    void rejectsOutOfRangeValues() throws Exception {
        postJson("{\"candidateType\":\"host\",\"rttMs\":-1,\"packetLossRatio\":0.01,\"jitterMs\":4}", 400);
        postJson("{\"candidateType\":\"host\",\"rttMs\":1,\"packetLossRatio\":1.5,\"jitterMs\":4}", 400);
        postJson("{\"candidateType\":\"host\",\"rttMs\":1}", 400);
    }
}
