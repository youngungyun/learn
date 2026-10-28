package com.example.meetlab.ice;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IceServerController.class)
class IceServerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IceServerService iceServerService;

    @Test
    void returnsIceServersWithoutCaching() throws Exception {
        given(iceServerService.issue(anyString())).willReturn(new IceServersResponse(
                List.of(new IceServer(List.of("stun:s:3478"), null, null)),
                Instant.parse("2026-10-29T01:00:00Z")));

        mockMvc.perform(get("/api/ice-servers"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.iceServers[0].urls[0]").value("stun:s:3478"))
                .andExpect(jsonPath("$.iceServers[0].username").doesNotExist());
    }
}
