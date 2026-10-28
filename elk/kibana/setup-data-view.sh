#!/usr/bin/env bash
# Kibana 에 meetlab-logs-* 데이터 뷰를 만든다. 한 번만 실행하면 된다.
# 사용: elk/kibana/setup-data-view.sh [KIBANA_URL]
set -euo pipefail
KIBANA="${1:-http://localhost:5601}"

curl -fsS -X POST "${KIBANA}/api/data_views/data_view" \
  -H 'kbn-xsrf: true' \
  -H 'Content-Type: application/json' \
  -d '{
        "data_view": {
          "id": "meetlab-logs",
          "title": "meetlab-logs-*",
          "name": "meetlab 로그",
          "timeFieldName": "@timestamp"
        },
        "override": true
      }'
echo
echo "데이터 뷰가 준비되었습니다: ${KIBANA}/app/discover"
