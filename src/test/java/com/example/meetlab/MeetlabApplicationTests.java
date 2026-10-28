package com.example.meetlab;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.meetlab.room.service.RoomService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

/**
 * 전체 컨텍스트를 올리는 스모크 테스트. 빈 설정이 깨졌는지 가장 빨리 알려 준다.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DisplayName("애플리케이션 컨텍스트")
class MeetlabApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("컨텍스트가 정상적으로 뜨고 핵심 빈이 등록된다")
    void contextLoads() {
        assertThat(applicationContext.getBean(RoomService.class)).isNotNull();
    }
}
