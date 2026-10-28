// REST API 부하 테스트: 방 생성/조회와 ICE 서버 발급이 동시 사용자에서도 빠른지 본다.
// "도착률(arrival rate)" 기반 실행기를 쓴다. 서버가 느려져도 초당 요청 수를 유지하므로
// 응답이 느려지는 순간 대기열이 쌓이는 모습까지 관찰할 수 있다.
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, JSON_HEADERS } from './lib/config.js';

export const options = {
  scenarios: {
    rooms: {
      executor: 'ramping-arrival-rate',
      startRate: 5,
      timeUnit: '1s',
      preAllocatedVUs: 20,
      maxVUs: 100,
      stages: [
        { target: 30, duration: '30s' },
        { target: 30, duration: '1m' },
        { target: 0, duration: '10s' },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    // 요청 종류별 임계값은 태그(name)로 나눠서 건다.
    'http_req_duration{name:create-room}': ['p(95)<300'],
    'http_req_duration{name:get-room}': ['p(95)<150'],
    'http_req_duration{name:ice-servers}': ['p(95)<100'],
    checks: ['rate>0.99'],
  },
};

export default function () {
  const created = http.post(`${BASE_URL}/api/rooms`, JSON.stringify({ name: 'k6 load', maxParticipants: 4 }), {
    headers: JSON_HEADERS,
    tags: { name: 'create-room' },
  });
  check(created, { 'created 201': (r) => r.status === 201 });
  if (created.status !== 201) {
    return;
  }

  const code = created.json('code');
  const fetched = http.get(`${BASE_URL}/api/rooms/${code}`, { tags: { name: 'get-room' } });
  check(fetched, { 'fetched 200': (r) => r.status === 200 });

  const ice = http.get(`${BASE_URL}/api/ice-servers`, { tags: { name: 'ice-servers' } });
  check(ice, { 'ice 200': (r) => r.status === 200 });
}
