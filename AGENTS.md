# meetlab — 에이전트 작업 지침

WebRTC 화상 통화 시그널링 서버(Spring Boot 4, Java 25). 이 문서는 사람과 AI 에이전트가 함께 읽는다.
**규칙은 모두 과거의 실패에서 나왔다.** 각 규칙의 근거는 `docs/harness/failure-log.md` 에 있고,
가능한 규칙은 문서가 아니라 코드(ArchUnit·훅·테스트)로 강제한다. 문서에만 있는 규칙은 지켜지지 않는다.

## 명령어

| 목적 | 명령 |
|---|---|
| 완료 판정(반드시 통과) | `./gradlew check` — 테스트 + JaCoCo 기준 + Checkstyle + SpotBugs |
| 빠른 피드백 | `./gradlew test --tests '*SignalingServiceTest'` |
| 스타일만 | `./gradlew checkstyleMain checkstyleTest` |
| 앱 실행(DB 포함) | `./gradlew bootTestRun` |
| 인프라 | `docker compose up -d postgres coturn prometheus grafana` |

테스트에는 Docker 가 필요하다(Testcontainers). Docker 가 없으면 "테스트를 못 돌렸다"고 보고하고 통과했다고 말하지 않는다.

## 구조

```
com.example.meetlab
├─ room        방 도메인  (domain → service → web 방향으로만 의존)
├─ signaling   WebSocket 시그널링 (room.service 만 사용)
├─ ice         STUN/TURN 설정과 TURN 임시 자격 증명
├─ telemetry   브라우저가 보고하는 통화 품질 지표
├─ logging     요청 ID(MDC) 필터
└─ search      Elasticsearch 방 검색 (room 이벤트를 구독해 색인, room 은 search 를 모른다)
```

- 의존 방향은 ArchUnit(`ArchitectureRulesTest`)이 검사한다. 패키지 간 순환 금지.
- 컨트롤러는 Repository 를 직접 쓰지 않는다. 서비스를 거친다.
- 시그널링 서버는 SDP/ICE 를 **해석하지 않고 전달만** 한다. 보낸 사람(`from`)은 서버가 채운다(위조 방지).

## 코드 규칙

- 의존성 주입은 생성자 주입만. 필드 `@Autowired` 금지.
- 로그는 SLF4J. `System.out`, `java.util.logging` 금지.
- 로그에는 구조화 필드를 쓴다: `LOG.atInfo().addKeyValue("room", code).log("입장")`. 문자열 이어붙이기로 값을 넣지 않는다. MDC 는 넣은 곳에서 `finally` 로 반드시 지운다.
- 검색어는 `QuerySanitizer` 를 거친 뒤에만 JSON 질의(`@Query`)에 넣는다.
- 지표 태그 값은 **닫힌 집합**만. 방 코드·peer id·클라이언트가 보낸 임의 문자열을 태그로 쓰지 않는다.
- 외부에서 온 값은 검증한 뒤에만 지표/로그/DB 에 쓴다. 검증은 Bean Validation(`@Valid`) 으로.
- 스키마 변경은 새 Flyway 파일(`V{다음번호}__설명.sql`) 추가로만. **이미 있는 V 파일은 수정 금지.**
- 엔티티 `save()` 는 식별자가 있으면 UPDATE(merge) 로 동작한다. "중복이면 실패"가 필요하면 DB 제약과 SQL 로 확인한다.

## 테스트 규칙

- 새 동작에는 테스트를 같이 추가한다. 버그 수정은 실패하는 테스트를 먼저 만든다.
- DB 가 필요한 테스트는 H2 를 쓰지 않고 `TestcontainersConfiguration` 을 `@Import` 한다.
- 시간·난수는 `Clock`, `RandomGenerator` 를 주입해 고정한다. `Thread.sleep` 으로 기다리지 않는다.
- 커버리지 기준(라인 75%, 브랜치 60%)을 낮추지 않는다. 기준을 맞추려고 의미 없는 테스트를 만들지도 않는다.

## 하지 말 것 (훅이 막는다)

- 품질 게이트 설정(`config/checkstyle`, `config/spotbugs`) 수정, 훅·`.claude/settings.json` 수정 — 사람의 승인 후에만.
- `.env` 편집·열람. 시크릿은 환경변수로만 주입한다.
- `git push --force`, `main` 직접 푸시, `--no-verify`, `git reset --hard`.
- `./gradlew ... -x test`, `-x check` 등 게이트 건너뛰기.
- `docker compose down -v` (DB·Grafana 볼륨 삭제).

## 완료의 정의 (Stop 훅이 확인한다)

1. `./gradlew check` 가 통과한다.
2. 변경한 동작에 해당하는 테스트가 있다.
3. 새 규칙이 필요한 실수를 했다면 `docs/harness/failure-log.md` 에 한 줄 추가하고, 가능하면 ArchUnit/훅/테스트로 만든다.

## 알려진 함정

- Spring Boot 4: 슬라이스 테스트 애너테이션은 기술별 패키지(`...webmvc.test.autoconfigure.WebMvcTest`)로 옮겨졌다. `@MockBean` 대신 `@MockitoBean`.
- Jackson 3: 패키지가 `tools.jackson.*`, 어노테이션만 `com.fasterxml.jackson.annotation.*` 그대로다.
- SpotBugs 는 한 번에 리포트 한 종류만 만든다(`-PspotbugsReport=xml|html`).
