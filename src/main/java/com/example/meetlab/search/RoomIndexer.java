package com.example.meetlab.search;

import com.example.meetlab.room.domain.RoomCreatedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 방 생성 트랜잭션이 <b>커밋된 뒤에만</b> 색인한다.
 *
 * <p>커밋 전에 색인하면 롤백된 방이 검색에 남는다. 반대로 색인이 실패했다고 방 생성까지 실패시키면
 * 검색 장애가 핵심 기능(방 만들기)을 막는다. 그래서 AFTER_COMMIT 에서 실행하고 실패는 지표와 로그로만 남긴다.
 * 빠진 문서는 {@link RoomReindexService} 로 복구한다.
 */
@Component
public class RoomIndexer {

    private static final Logger LOG = LoggerFactory.getLogger(RoomIndexer.class);

    private final RoomSearchRepository repository;
    private final MeterRegistry meterRegistry;

    public RoomIndexer(RoomSearchRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoomCreated(RoomCreatedEvent event) {
        try {
            repository.save(RoomDocument.from(event));
            meterRegistry.counter("meetlab.search.index", "result", "success").increment();
        } catch (RuntimeException e) {
            meterRegistry.counter("meetlab.search.index", "result", "failure").increment();
            LOG.warn("방 색인 실패(재색인으로 복구 가능): code={}, cause={}", event.code(), e.toString());
        }
    }
}
