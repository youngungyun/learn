# meetlab

WebRTC 화상 통화용 시그널링 서버를 단계별로 만들면서 품질, 테스트, 모니터링, 검색, 로그를 쌓아 가는 학습 프로젝트입니다.
전체 학습 순서와 단계별 코드 전문은 `CURRICULUM.md`에 있습니다.

## 체크포인트(git 태그)

| 태그 | 내용 |
|---|---|
| `stage-0a` | 테스트, JaCoCo, Checkstyle, SpotBugs 품질 게이트 |
| `stage-0b` | Jenkins 파이프라인 |
| `stage-1` | WebRTC 시그널링 + TURN + 브라우저 클라이언트 |
| `stage-2` | Testcontainers (PostgreSQL) |
| `stage-3` | Micrometer, Prometheus, Grafana |
| `stage-4` | k6 부하 테스트 |
| `stage-5` | 하네스 엔지니어링 (AGENTS.md, ArchUnit, hooks) |
| `stage-6` | Elasticsearch 방 검색 |
| `stage-7` | ELK 로그 수집 |

막히면 `git diff stage-N-1 stage-N` 으로 해당 단계에서 바뀐 부분만 볼 수 있습니다.

## 자주 쓰는 명령

```bash
./gradlew clean check          # 테스트 + 커버리지 기준 + Checkstyle + SpotBugs
./gradlew bootRun              # 서버 실행 (http://localhost:8080)
```
