// 스모크 테스트: 배포 직후 "일단 살아 있는가"를 1명의 가상 사용자로 빠르게 확인한다.
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, JSON_HEADERS } from './lib/config.js';

export const options = {
  vus: 1,
  iterations: 5,
  thresholds: {
    http_req_failed: ['rate==0'],
    http_req_duration: ['p(95)<500'],
    checks: ['rate==1'],
  },
};

export default function () {
  const health = http.get(`${BASE_URL}/actuator/health`);
  check(health, { 'health 200': (r) => r.status === 200, 'health UP': (r) => r.json('status') === 'UP' });

  const created = http.post(`${BASE_URL}/api/rooms`, JSON.stringify({ name: 'k6 smoke', maxParticipants: 2 }), {
    headers: JSON_HEADERS,
  });
  check(created, { 'room created 201': (r) => r.status === 201 });

  const code = created.json('code');
  const fetched = http.get(`${BASE_URL}/api/rooms/${code}`);
  check(fetched, { 'room fetched 200': (r) => r.status === 200, 'same code': (r) => r.json('code') === code });

  const ice = http.get(`${BASE_URL}/api/ice-servers`);
  check(ice, { 'ice-servers 200': (r) => r.status === 200, 'no-store': (r) => r.headers['Cache-Control'] === 'no-store' });

  sleep(0.2);
}
