package com.example.meetlab.search;

import com.example.meetlab.room.domain.Room;
import com.example.meetlab.room.domain.RoomRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** DB(원본)의 모든 방을 다시 색인한다. 색인 실패 복구, 분석기 변경 후 재구축에 쓴다. */
@Service
public class RoomReindexService {

    static final int BATCH_SIZE = 500;

    private final RoomRepository roomRepository;
    private final RoomSearchRepository searchRepository;

    public RoomReindexService(RoomRepository roomRepository, RoomSearchRepository searchRepository) {
        this.roomRepository = roomRepository;
        this.searchRepository = searchRepository;
    }

    /** @return 색인한 방의 수 */
    @Transactional(readOnly = true)
    public long reindexAll() {
        long total = 0;
        int pageNumber = 0;
        Page<Room> page;
        do {
            // 정렬 기준이 없으면 페이지 사이에 순서가 바뀌어 누락·중복이 생길 수 있다.
            page = roomRepository.findAll(PageRequest.of(pageNumber++, BATCH_SIZE, Sort.by("createdAt", "code")));
            List<RoomDocument> documents = page.getContent().stream().map(RoomDocument::from).toList();
            searchRepository.saveAll(documents);
            total += documents.size();
        } while (page.hasNext());
        return total;
    }
}
