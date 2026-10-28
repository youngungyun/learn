package com.example.meetlab.search;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class RoomSearchController {

    private final RoomSearchService searchService;
    private final RoomReindexService reindexService;

    public RoomSearchController(RoomSearchService searchService, RoomReindexService reindexService) {
        this.searchService = searchService;
        this.reindexService = reindexService;
    }

    @GetMapping("/rooms")
    public List<RoomSearchResult> search(@RequestParam("q") String query,
            @RequestParam(value = "size", required = false) Integer size) {
        return searchService.search(query, size);
    }

    /**
     * 전체 재색인. 인증이 없는 학습용이라 열어 두었지만 운영에서는 반드시 관리자 권한으로 보호해야 한다.
     */
    @PostMapping("/rooms/reindex")
    public Map<String, Long> reindex() {
        return Map.of("indexed", reindexService.reindexAll());
    }
}
