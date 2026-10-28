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

## stage 1 실행 방법 (WebRTC)

```bash
cp .env.example .env
docker compose up -d coturn          # STUN/TURN
./gradlew bootRun                    # http://localhost:8080
```

브라우저 탭 두 개에서 `http://localhost:8080` 을 열고, 한쪽에서 "방 만들기" 후 같은 코드로 두 탭이 입장한다.

## stage 2 실행 방법 (Testcontainers)

```bash
./gradlew test            # Docker 가 떠 있어야 한다. PostgreSQL 18 컨테이너가 자동으로 뜬다.
./gradlew bootTestRun     # docker compose 없이 Testcontainers DB 로 앱 실행
docker compose up -d postgres && ./gradlew bootRun   # compose 의 DB 로 실행
```

## stage 3 실행 방법 (모니터링)

```bash
docker compose up -d postgres prometheus grafana
./gradlew bootRun
# 앱 지표      http://localhost:8080/actuator/prometheus
# Prometheus   http://localhost:9090   (Status > Targets 에서 meetlab 이 UP 인지 확인)
# Grafana      http://localhost:3000   (admin / admin, "meetlab 개요" 대시보드)
```

## stage 4 실행 방법 (k6)

```bash
docker compose up -d postgres prometheus grafana
./gradlew bootRun
# 결과를 Prometheus 로 보내며 실행 (Grafana "meetlab k6 부하 테스트" 대시보드에서 실시간 확인)
docker compose run --rm k6 run -o experimental-prometheus-rw /scripts/smoke.js
docker compose run --rm k6 run -o experimental-prometheus-rw /scripts/api-load.js
docker compose run --rm k6 run -o experimental-prometheus-rw /scripts/signaling-load.js
```

## stage 5 실행 방법 (하네스 엔지니어링)

```bash
python3 -m unittest harness/test_hooks.py -v   # 훅 스크립트 단위 테스트(Docker/Gradle 불필요)
./gradlew test --tests '*ArchitectureRulesTest' --tests '*HarnessFilesTest'
claude    # 프로젝트 루트에서 Claude Code 를 실행하면 AGENTS.md 와 .claude/settings.json 의 훅이 적용된다
```

## stage 6 실행 방법 (Elasticsearch)

```bash
docker compose up -d --build postgres elasticsearch   # nori 포함 이미지를 빌드한다(첫 실행은 수 분)
./gradlew bootRun
curl -XPOST localhost:8080/api/rooms -H 'Content-Type: application/json' -d '{"name":"화상 회의실"}'
curl 'localhost:8080/api/search/rooms?q=회의'
curl -XPOST localhost:8080/api/search/rooms/reindex     # DB 기준 전체 재색인
curl -XPOST 'localhost:9200/rooms/_analyze' -H 'Content-Type: application/json' -d '{"analyzer":"korean","text":"화상회의실"}'
```

## stage 7 실행 방법 (ELK 로그)

```bash
docker compose --profile elk up -d --build   # elasticsearch + logstash + filebeat + kibana (메모리 6GB 이상 권장)
./gradlew bootRun                            # logs/meetlab.log 에 ECS JSON 로그가 쌓인다
elk/kibana/setup-data-view.sh                # Kibana 데이터 뷰 생성(최초 1회)
# http://localhost:5601/app/discover  →  KQL 예) room : "abc-defg-hij"   /   log.level : "ERROR"
python3 -m unittest elk/test_pipeline_mapping.py -v   # Logstash 필드 매핑 검증(도커 불필요)
```
