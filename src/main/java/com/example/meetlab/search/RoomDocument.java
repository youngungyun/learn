package com.example.meetlab.search;

import com.example.meetlab.room.domain.Room;
import com.example.meetlab.room.domain.RoomCreatedEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

/**
 * Elasticsearch 에 저장하는 방 문서. DB 의 Room 이 원본이고, 이 문서는 검색용 사본이다.
 *
 * <p>분석기(nori)와 필드 매핑은 코드가 아니라 JSON 파일에 둔다. 분석기 설정은 길고 ES 문서와 같은 모양이어야
 * 복사해 실험하기 쉽다. 문서 ID 가 방 코드이므로 같은 방을 여러 번 색인해도 덮어쓰기만 일어난다(멱등).
 *
 * @param createdAt epoch milliseconds (매핑의 date/epoch_millis 와 맞춘다)
 */
@Document(indexName = "rooms", writeTypeHint = WriteTypeHint.FALSE)
@Setting(settingPath = "/elasticsearch/room-settings.json")
@Mapping(mappingPath = "/elasticsearch/room-mapping.json")
public record RoomDocument(@Id String code, String name, int maxParticipants, long createdAt) {

    public static RoomDocument from(RoomCreatedEvent event) {
        return new RoomDocument(event.code(), event.name(), event.maxParticipants(), event.createdAt().toEpochMilli());
    }

    public static RoomDocument from(Room room) {
        return new RoomDocument(room.getCode(), room.getName(), room.getMaxParticipants(),
                room.getCreatedAt().toEpochMilli());
    }
}
