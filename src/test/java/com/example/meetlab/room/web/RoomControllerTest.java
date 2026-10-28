package com.example.meetlab.room.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.meetlab.room.RoomFixture;
import com.example.meetlab.room.exception.RoomNotFoundException;
import com.example.meetlab.room.service.RoomService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 웹 계층만 올리는 슬라이스 테스트. 서비스는 @MockitoBean으로 대체한다.
 * (Spring Boot 4에서는 @MockBean이 제거되고 @MockitoBean이 표준이다.)
 */
@WebMvcTest(RoomController.class)
@DisplayName("RoomController")
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomService roomService;

    @Test
    @DisplayName("방을 만들면 201과 Location 헤더를 반환한다")
    void createReturnsCreated() throws Exception {
        given(roomService.create("주간 회의", 3)).willReturn(RoomFixture.room("abc-defg-hij", "주간 회의", 3));

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "주간 회의", "maxParticipants": 3}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/rooms/abc-defg-hij"))
                .andExpect(jsonPath("$.code").value("abc-defg-hij"))
                .andExpect(jsonPath("$.name").value("주간 회의"))
                .andExpect(jsonPath("$.maxParticipants").value(3));
    }

    @Test
    @DisplayName("최대 인원을 생략해도 만들 수 있다")
    void createAllowsOmittedCapacity() throws Exception {
        given(roomService.create("회의", null)).willReturn(RoomFixture.room("abc-defg-hij", "회의", 4));

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "회의"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maxParticipants").value(4));
    }

    @Test
    @DisplayName("이름이 비어 있으면 400이고 서비스는 호출되지 않는다")
    void createRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": " "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        then(roomService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("최대 인원이 범위를 벗어나면 400이다")
    void createRejectsOutOfRangeCapacity() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "회의", "maxParticipants": 5}
                                """))
                .andExpect(status().isBadRequest());

        then(roomService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("서비스가 IllegalArgumentException을 던지면 400과 사유를 반환한다")
    void illegalArgumentBecomesBadRequest() throws Exception {
        given(roomService.create("회의", 2)).willThrow(new IllegalArgumentException("잘못된 요청"));

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "회의", "maxParticipants": 2}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("잘못된 요청"));
    }

    @Test
    @DisplayName("방 조회에 성공한다")
    void getReturnsRoom() throws Exception {
        given(roomService.get("abc-defg-hij")).willReturn(RoomFixture.room("abc-defg-hij"));

        mockMvc.perform(get("/api/rooms/abc-defg-hij"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("주간 회의"));
    }

    @Test
    @DisplayName("없는 방을 조회하면 404를 반환한다")
    void getReturnsNotFound() throws Exception {
        given(roomService.get("zzz-zzzz-zzz")).willThrow(new RoomNotFoundException("zzz-zzzz-zzz"));

        mockMvc.perform(get("/api/rooms/zzz-zzzz-zzz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("최근 방 목록을 반환한다")
    void recentReturnsList() throws Exception {
        given(roomService.recent()).willReturn(List.of(RoomFixture.room("abc-defg-hij")));

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("abc-defg-hij"));
    }
}
