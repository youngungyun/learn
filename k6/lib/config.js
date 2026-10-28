// 모든 시나리오가 공유하는 설정. 환경변수로 바꿀 수 있다.
//   k6 run -e BASE_URL=http://localhost:8080 k6/smoke.js
export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const WS_URL = (__ENV.WS_URL || BASE_URL.replace(/^http/, 'ws')) + '/ws/signaling';
// 서버가 허용하는 Origin(localhost 계열)과 맞춘다. 브라우저가 아니라 k6 가 접속하므로 직접 지정한다.
export const ORIGIN = __ENV.ORIGIN || 'http://localhost:8080';
export const JSON_HEADERS = { 'Content-Type': 'application/json' };
