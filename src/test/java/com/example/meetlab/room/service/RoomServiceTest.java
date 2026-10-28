package com.example.meetlab.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.example.meetlab.room.RoomFixture;
import com.example.meetlab.room.domain.Room;
import com.example.meetlab.room.domain.RoomCodeGenerator;
import com.example.meetlab.room.domain.RoomCreatedEvent;
import com.example.meetlab.room.domain.RoomRepository;
import com.example.meetlab.room.exception.RoomNotFoundException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomService")
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomCodeGenerator codeGenerator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RoomService roomService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(RoomFixture.NOW, ZoneOffset.UTC);
        roomService = new RoomService(roomRepository, codeGenerator, fixedClock, eventPublisher);
    }

    @Test
    @DisplayName("사용 가능한 코드로 방을 저장한다")
    void createSavesRoomWithGeneratedCode() {
        given(codeGenerator.generate()).willReturn("abc-defg-hij");
        given(roomRepository.existsById("abc-defg-hij")).willReturn(false);
        given(roomRepository.save(any(Room.class))).willAnswer(invocation -> invocation.getArgument(0));

        Room created = roomService.create("주간 회의", 3);

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        then(roomRepository).should().save(captor.capture());
        Room saved = captor.getValue();
        assertThat(saved.getCode()).isEqualTo("abc-defg-hij");
        assertThat(saved.getName()).isEqualTo("주간 회의");
        assertThat(saved.getMaxParticipants()).isEqualTo(3);
        assertThat(saved.getCreatedAt()).isEqualTo(RoomFixture.NOW);
        assertThat(created).isSameAs(saved);
    }

    @Test
    @DisplayName("최대 인원을 생략하면 4명으로 만든다")
    void createUsesMaximumCapacityWhenOmitted() {
        given(codeGenerator.generate()).willReturn("abc-defg-hij");
        given(roomRepository.existsById("abc-defg-hij")).willReturn(false);
        given(roomRepository.save(any(Room.class))).willAnswer(invocation -> invocation.getArgument(0));

        Room created = roomService.create("회의", null);

        assertThat(created.getMaxParticipants()).isEqualTo(Room.MAX_PARTICIPANTS);
    }

    @Test
    @DisplayName("코드가 이미 있으면 다시 생성해서 빈 코드를 찾는다")
    void createRetriesOnCodeCollision() {
        given(codeGenerator.generate()).willReturn("aaa-aaaa-aaa", "bbb-bbbb-bbb");
        given(roomRepository.existsById("aaa-aaaa-aaa")).willReturn(true);
        given(roomRepository.existsById("bbb-bbbb-bbb")).willReturn(false);
        given(roomRepository.save(any(Room.class))).willAnswer(invocation -> invocation.getArgument(0));

        Room created = roomService.create("회의", 2);

        assertThat(created.getCode()).isEqualTo("bbb-bbbb-bbb");
    }

    @Test
    @DisplayName("계속 충돌하면 5번 시도한 뒤 포기한다")
    void createGivesUpAfterMaxAttempts() {
        given(codeGenerator.generate()).willReturn("aaa-aaaa-aaa");
        given(roomRepository.existsById("aaa-aaaa-aaa")).willReturn(true);

        assertThatIllegalStateException().isThrownBy(() -> roomService.create("회의", 2));

        then(codeGenerator).should(times(RoomService.MAX_CODE_ATTEMPTS)).generate();
        then(roomRepository).should(never()).save(any(Room.class));
    }

    @Test
    @DisplayName("이름이 잘못되면 저장하지 않는다")
    void createRejectsInvalidName() {
        given(codeGenerator.generate()).willReturn("abc-defg-hij");
        given(roomRepository.existsById("abc-defg-hij")).willReturn(false);

        assertThatIllegalArgumentException().isThrownBy(() -> roomService.create("  ", 2));

        then(roomRepository).should(never()).save(any(Room.class));
    }

    @Test
    @DisplayName("코드로 방을 조회한다")
    void getReturnsRoom() {
        Room room = RoomFixture.room("abc-defg-hij");
        given(roomRepository.findById("abc-defg-hij")).willReturn(Optional.of(room));

        assertThat(roomService.get("abc-defg-hij")).isSameAs(room);
    }

    @Test
    @DisplayName("없는 코드를 조회하면 RoomNotFoundException")
    void getThrowsWhenMissing() {
        given(roomRepository.findById("zzz-zzzz-zzz")).willReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.get("zzz-zzzz-zzz"))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessageContaining("zzz-zzzz-zzz");
    }

    @Test
    @DisplayName("최근 방 목록은 저장소 결과를 그대로 돌려준다")
    void recentDelegatesToRepository() {
        List<Room> rooms = List.of(RoomFixture.room("abc-defg-hij"));
        given(roomRepository.findTop10ByOrderByCreatedAtDesc()).willReturn(rooms);

        assertThat(roomService.recent()).isEqualTo(rooms);
    }

    @Test
    @DisplayName("방을 만들면 RoomCreatedEvent 를 발행한다")
    void createPublishesRoomCreatedEvent() {
        given(codeGenerator.generate()).willReturn("abc-defg-hij");
        given(roomRepository.existsById("abc-defg-hij")).willReturn(false);
        given(roomRepository.save(any(Room.class))).willAnswer(invocation -> invocation.getArgument(0));

        roomService.create("주간 회의", 3);

        ArgumentCaptor<RoomCreatedEvent> captor = ArgumentCaptor.forClass(RoomCreatedEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        assertThat(captor.getValue().code()).isEqualTo("abc-defg-hij");
        assertThat(captor.getValue().name()).isEqualTo("주간 회의");
        assertThat(captor.getValue().createdAt()).isEqualTo(RoomFixture.NOW);
    }
}
