-- 화상 통화 방. 엔티티(Room)와 컬럼 이름·타입이 같아야 ddl-auto=validate 를 통과한다.
CREATE TABLE rooms (
    code             VARCHAR(20)              NOT NULL,
    name             VARCHAR(50)              NOT NULL,
    max_participants INTEGER                  NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_rooms PRIMARY KEY (code),
    -- 애플리케이션 검증이 우회되더라도 DB 가 마지막으로 막는다. (Room.MIN/MAX_PARTICIPANTS 와 같은 값)
    CONSTRAINT ck_rooms_max_participants CHECK (max_participants BETWEEN 2 AND 4)
);

CREATE INDEX idx_rooms_created_at ON rooms (created_at DESC);
