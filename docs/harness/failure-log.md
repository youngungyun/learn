# 실패 기록 → 규칙 (Failure Log)

하네스 엔지니어링의 핵심 순환: **실패가 생긴다 → 원인을 한 줄로 적는다 → 사람이 아니라 시스템이 막게 만든다.**
규칙이 이 표의 한 줄과 연결되지 않으면 추측으로 만든 규칙이므로 지운다.

> 아래 항목은 이 커리큘럼을 만들며 실제로 부딪혔거나 설계 단계에서 걸러낸 함정이다(운영 사고가 아니라 학습용 출발점).
> 프로젝트를 하다 새 실수를 하면 맨 아래에 한 줄 추가한다.

| ID | 무슨 일이 있었나 | 원인 | 규칙 | 강제 수단 |
|---|---|---|---|---|
| F-001 | 중복 방 코드 저장이 에러 없이 기존 방을 덮어쓸 수 있었다 | 식별자가 있는 엔티티의 `save()` 는 INSERT 가 아니라 merge(UPDATE) | 중복 방지는 DB 제약 + SQL 로 검증한다 | `RoomRepositoryTest.rejectsDuplicateCode`, AGENTS.md "코드 규칙" |
| F-002 | 최대 인원 검증이 애플리케이션에만 있어 우회 경로가 생길 수 있었다 | 검증이 한 계층에만 있음 | 핵심 불변식은 DB CHECK 제약으로도 건다 | `V1__create_rooms.sql`, `RoomRepositoryTest.rejectsOutOfRangeCapacity` |
| F-003 | 클라이언트가 보낸 `type` 문자열을 그대로 지표 태그로 쓰면 시계열이 무한히 늘어난다 | 태그 카디널리티 | 지표 태그는 닫힌 집합만 | `SignalingServiceTest.unknownTypeIsBucketedToAvoidHighCardinalityTags`, `WebRtcStatsRequest` 검증 |
| F-004 | 시그널링 메시지의 보낸 사람을 클라이언트 값으로 믿으면 다른 사람 행세가 가능하다 | 신뢰 경계 | `from` 은 서버가 채운다 | `ClientMessage` 에 from 필드 없음, `SignalingServiceTest.offerIsRelayedWithServerFilledSender` |
| F-005 | 이미 적용된 Flyway 파일을 고치면 체크섬 불일치로 기동이 실패한다 | 마이그레이션은 불변 이력 | 기존 V 파일 수정 금지, 새 파일 추가 | `guard_edit.py` 훅 |
| F-006 | 품질 게이트가 실패하자 규칙(예외 목록)을 느슨하게 고치는 쪽으로 가기 쉽다 | 목표(통과)와 수단(게이트)의 혼동 | 게이트 설정은 사람 승인 후에만 수정 | `guard_edit.py` 훅 |
| F-007 | `./gradlew check -x test` 로 "통과"를 만들 수 있다 | 검증 건너뛰기 | `-x` 로 게이트 건너뛰기 금지 | `guard_bash.py` 훅 |
| F-008 | "끝났다"고 보고했지만 `check` 는 실패 상태였다 | 완료 선언을 검증 없이 수용 | 끝내기 전에 `check` 강제 | `stop_gate.py` (Stop 훅) |
| F-009 | 컨트롤러가 Repository 를 직접 부르기 시작하면 트랜잭션·검증 규칙이 우회된다 | 계층 경계 약화 | 컨트롤러 → 서비스 → 리포지토리 | `ArchitectureRulesTest` |
| F-010 | Boot 4 에서 `TestRestTemplate`·슬라이스 테스트 패키지가 바뀌어 예전 코드가 컴파일되지 않는다 | 메이저 버전 변경 | 통합 테스트는 JDK `HttpClient` 사용, 슬라이스 패키지는 AGENTS.md "알려진 함정" 참고 | AGENTS.md |

## 새 항목을 추가하는 법

1. 무슨 일이 있었는지, **왜** 일어났는지(원인)를 적는다. "조심하자"는 원인이 아니다.
2. 규칙을 가장 강한 수단으로 만든다: 컴파일/ArchUnit/테스트 > 훅 > 문서.
3. 강제 수단 칸에 그 파일 이름을 적는다. 수단이 문서뿐이면 강제할 방법을 더 찾는다.
