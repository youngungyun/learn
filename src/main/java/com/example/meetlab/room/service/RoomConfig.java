package com.example.meetlab.room.service;

import com.example.meetlab.room.domain.RoomCodeGenerator;
import java.security.SecureRandom;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RoomConfig {

    @Bean
    public RoomCodeGenerator roomCodeGenerator() {
        return new RoomCodeGenerator(new SecureRandom());
    }

    /** 시간을 주입 가능하게 만들어 두면 테스트에서 고정된 시각을 쓸 수 있다. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
