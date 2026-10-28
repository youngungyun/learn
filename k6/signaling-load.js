// 시그널링 부하 테스트: 가상 사용자 한 명이 "방 생성 -> 두 명이 WebSocket 으로 입장 -> offer 중계 -> 잠시 유지 -> 종료"
// 한 세션을 반복한다. 입장(join) 응답 시간과 offer 중계 시간을 별도 지표로 잰다.
import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { clearTimeout, setTimeout } from 'k6/timers';
import { WebSocket } from 'k6/websockets';
import { JSON_HEADERS, BASE_URL, ORIGIN, WS_URL } from './lib/config.js';

const joinLatency = new Trend('signaling_join_ms', true);
const relayLatency = new Trend('signaling_relay_ms', true);
const sessionsCompleted = new Counter('signaling_sessions_completed');
const sessionsFailed = new Counter('signaling_sessions_failed');

const HOLD_MS = Number(__ENV.HOLD_MS || 3000); // 연결을 유지하는 시간. 길수록 meetlab_peers_active 가 높게 관찰된다.
const SESSION_TIMEOUT_MS = 15000;
const FAKE_SDP = { type: 'offer', sdp: 'v=0\r\n' + 'a=fake\r\n'.repeat(250) }; // 약 2KB, 실제 SDP 와 비슷한 크기

export const options = {
  scenarios: {
    sessions: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 20 },
        { duration: '15s', target: 0 },
      ],
      gracefulRampDown: '20s',
    },
  },
  thresholds: {
    signaling_join_ms: ['p(95)<200'],
    signaling_relay_ms: ['p(95)<200'],
    signaling_sessions_failed: ['count==0'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
};

function open() {
  return new WebSocket(WS_URL, null, { headers: { Origin: ORIGIN } });
}

function send(ws, message) {
  ws.send(JSON.stringify(message));
}

function runSession(code) {
  const alice = open();
  const bob = open();
  let bobOpen = false;
  let aliceJoined = false;
  let joinSentAt = 0;
  let offerSentAt = 0;
  let finished = false;
  let bobId = null;
  let holdTimer = null;

  const finish = (ok) => {
    if (finished) {
      return;
    }
    finished = true;
    clearTimeout(deadline);
    if (holdTimer !== null) {
      clearTimeout(holdTimer);
    }
    (ok ? sessionsCompleted : sessionsFailed).add(1);
    alice.close();
    bob.close();
  };
  const deadline = setTimeout(() => finish(false), SESSION_TIMEOUT_MS);

  const startBobIfReady = () => {
    if (aliceJoined && bobOpen) {
      send(bob, { type: 'join', room: code, displayName: 'bob' });
    }
  };

  alice.onopen = () => {
    joinSentAt = Date.now();
    send(alice, { type: 'join', room: code, displayName: 'alice' });
  };
  alice.onmessage = (event) => {
    const message = JSON.parse(event.data);
    if (message.type === 'joined') {
      joinLatency.add(Date.now() - joinSentAt);
      aliceJoined = true;
      startBobIfReady();
    } else if (message.type === 'offer') {
      relayLatency.add(Date.now() - offerSentAt);
      const ok = check(message, { 'offer from bob': (m) => m.from === bobId, 'sdp intact': (m) => m.data.sdp === FAKE_SDP.sdp });
      if (!ok) {
        finish(false);
        return;
      }
      holdTimer = setTimeout(() => finish(true), HOLD_MS);
    } else if (message.type === 'error') {
      finish(false);
    }
  };
  bob.onopen = () => {
    bobOpen = true;
    startBobIfReady();
  };
  bob.onmessage = (event) => {
    const message = JSON.parse(event.data);
    if (message.type === 'joined') {
      bobId = message.id;
      const aliceId = message.peers[0] && message.peers[0].id;
      offerSentAt = Date.now();
      send(bob, { type: 'offer', to: aliceId, data: FAKE_SDP });
    } else if (message.type === 'error') {
      finish(false);
    }
  };
  alice.onerror = () => finish(false);
  bob.onerror = () => finish(false);
}

export default function () {
  const created = http.post(`${BASE_URL}/api/rooms`, JSON.stringify({ name: 'k6 signaling', maxParticipants: 2 }), {
    headers: JSON_HEADERS,
  });
  if (!check(created, { 'room created': (r) => r.status === 201 })) {
    sessionsFailed.add(1);
    return;
  }
  runSession(created.json('code'));
}
