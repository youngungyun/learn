// 메시 방식 WebRTC 클라이언트. 새로 들어온 쪽이 기존 참가자 모두에게 offer 를 보낸다.
const $ = (id) => document.getElementById(id);
const statusEl = $('status');
const videos = $('videos');

let ws = null;
let localStream = null;
let myId = null;
let iceServers = [];
const peers = new Map(); // peerId -> { pc, figure, pendingCandidates }

function setStatus(text) {
  statusEl.textContent = text;
}

async function fetchIceServers() {
  const res = await fetch('/api/ice-servers', { cache: 'no-store' });
  if (!res.ok) throw new Error('ICE 서버 정보를 가져오지 못했습니다.');
  return (await res.json()).iceServers;
}

async function createRoom() {
  const res = await fetch('/api/rooms', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name: '데모 방', maxParticipants: 4 }),
  });
  if (!res.ok) throw new Error('방을 만들지 못했습니다.');
  return (await res.json()).code;
}

function send(message) {
  if (ws && ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(message));
}

function addRemoteTile(peerId, label) {
  const figure = document.createElement('figure');
  const video = document.createElement('video');
  video.autoplay = true;
  video.playsInline = true;
  const caption = document.createElement('figcaption');
  caption.textContent = label || peerId;
  figure.append(video, caption);
  videos.append(figure);
  return { figure, video };
}

function createPeer(peerId, label) {
  const pc = new RTCPeerConnection({ iceServers });
  const tile = addRemoteTile(peerId, label);
  const entry = { pc, figure: tile.figure, pendingCandidates: [] };
  peers.set(peerId, entry);

  localStream.getTracks().forEach((track) => pc.addTrack(track, localStream));
  pc.ontrack = (event) => {
    tile.video.srcObject = event.streams[0];
  };
  pc.onicecandidate = (event) => {
    if (event.candidate) send({ type: 'candidate', to: peerId, data: event.candidate });
  };
  pc.onconnectionstatechange = () => {
    if (pc.connectionState === 'failed') pc.restartIce();
  };
  return entry;
}

async function flushCandidates(entry) {
  for (const candidate of entry.pendingCandidates.splice(0)) {
    await entry.pc.addIceCandidate(candidate);
  }
}

async function onOffer(from, offer) {
  const entry = peers.get(from) ?? createPeer(from, from);
  await entry.pc.setRemoteDescription(offer);
  await flushCandidates(entry);
  const answer = await entry.pc.createAnswer();
  await entry.pc.setLocalDescription(answer);
  send({ type: 'answer', to: from, data: entry.pc.localDescription });
}

async function onAnswer(from, answer) {
  const entry = peers.get(from);
  if (!entry) return;
  await entry.pc.setRemoteDescription(answer);
  await flushCandidates(entry);
}

async function onCandidate(from, candidate) {
  const entry = peers.get(from);
  if (!entry) return;
  if (entry.pc.remoteDescription) {
    await entry.pc.addIceCandidate(candidate);
  } else {
    entry.pendingCandidates.push(candidate); // remoteDescription 이 생기기 전에 온 후보는 잠시 보관
  }
}

// 5초마다 통화 품질(RTT, 손실, 지터)을 뽑아 서버로 보낸다. 서버는 이를 Prometheus 지표로 만든다.
async function reportStats(entry) {
  const report = await entry.pc.getStats();
  let pair = null;
  let inbound = null;
  report.forEach((stat) => {
    if (stat.type === 'candidate-pair' && stat.state === 'succeeded' && stat.nominated) pair = stat;
    if (stat.type === 'inbound-rtp' && stat.kind === 'video') inbound = stat;
  });
  if (!pair || !inbound || pair.currentRoundTripTime == null) return;
  const local = report.get(pair.localCandidateId);
  const received = inbound.packetsReceived + inbound.packetsLost;
  await fetch('/api/telemetry/webrtc', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      candidateType: local?.candidateType ?? 'host',
      rttMs: pair.currentRoundTripTime * 1000,
      packetLossRatio: received > 0 ? Math.min(1, inbound.packetsLost / received) : 0,
      jitterMs: (inbound.jitter ?? 0) * 1000,
    }),
  });
}

setInterval(() => {
  peers.forEach((entry) => reportStats(entry).catch(() => { /* 보고 실패가 통화를 막으면 안 된다 */ }));
}, 5000);

function removePeer(peerId) {
  const entry = peers.get(peerId);
  if (!entry) return;
  entry.pc.close();
  entry.figure.remove();
  peers.delete(peerId);
}

async function onMessage(message) {
  switch (message.type) {
    case 'joined':
      myId = message.id;
      setStatus(`입장 완료 (기존 ${message.peers.length}명)`);
      for (const peer of message.peers) {
        const entry = createPeer(peer.id, peer.displayName);
        const offer = await entry.pc.createOffer();
        await entry.pc.setLocalDescription(offer);
        send({ type: 'offer', to: peer.id, data: entry.pc.localDescription });
      }
      break;
    case 'peer-joined':
      setStatus(`${message.displayName} 님이 입장했습니다.`);
      break; // 새로 들어온 쪽이 offer 를 보내므로 여기서는 기다리기만 한다.
    case 'peer-left':
      removePeer(message.id);
      break;
    case 'offer':
      await onOffer(message.from, message.data);
      break;
    case 'answer':
      await onAnswer(message.from, message.data);
      break;
    case 'candidate':
      await onCandidate(message.from, message.data);
      break;
    case 'error':
      setStatus(`오류: ${message.message}`);
      break;
    default:
      break;
  }
}

async function join(roomCode, displayName) {
  localStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
  $('local').srcObject = localStream;
  iceServers = await fetchIceServers();

  const protocol = location.protocol === 'https:' ? 'wss' : 'ws';
  ws = new WebSocket(`${protocol}://${location.host}/ws/signaling`);
  ws.onopen = () => send({ type: 'join', room: roomCode, displayName });
  ws.onmessage = (event) => onMessage(JSON.parse(event.data)).catch((e) => setStatus(e.message));
  ws.onclose = () => leave(false);
  $('leave').disabled = false;
}

function leave(notify = true) {
  if (notify) send({ type: 'leave' });
  if (ws) ws.close();
  ws = null;
  [...peers.keys()].forEach(removePeer);
  if (localStream) localStream.getTracks().forEach((track) => track.stop());
  localStream = null;
  $('local').srcObject = null;
  $('leave').disabled = true;
  setStatus('연결이 종료되었습니다.');
}

$('join-form').addEventListener('submit', (event) => {
  event.preventDefault();
  join($('room-code').value.trim(), $('display-name').value.trim()).catch((e) => setStatus(e.message));
});
$('create-room').addEventListener('click', () => {
  createRoom().then((code) => { $('room-code').value = code; setStatus(`방 코드: ${code}`); })
    .catch((e) => setStatus(e.message));
});
$('leave').addEventListener('click', () => leave(true));
