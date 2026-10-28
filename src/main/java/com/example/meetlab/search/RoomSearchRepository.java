package com.example.meetlab.search;

import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface RoomSearchRepository extends ElasticsearchRepository<RoomDocument, String> {

    /**
     * 형태소 단위 일치(name, 가중치 3)와 앞부분 일치(name.prefix)를 합쳐 점수가 높은 순으로 찾는다.
     * ?0 자리에는 {@link QuerySanitizer} 를 거친 문자열만 넣는다.
     */
    @Query("""
            {
              "bool": {
                "should": [
                  { "match": { "name": { "query": "?0", "boost": 3 } } },
                  { "match": { "name.prefix": { "query": "?0" } } }
                ],
                "minimum_should_match": 1
              }
            }
            """)
    SearchHits<RoomDocument> search(String text, Pageable pageable);
}
