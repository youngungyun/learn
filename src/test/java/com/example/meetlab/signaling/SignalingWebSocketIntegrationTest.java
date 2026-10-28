package com.example.meetlab.signaling;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.meetlab.IntegrationTest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 실제 서버를 띄워 WebSocket 두 개가 방 하나에서 시그널링을 주고받는지 확인한다. */
@IntegrationTest
class SignalingWebSocketIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Value("${local.server.port}")
    private int port;

    private static final class Client implements WebSocket.Listener {
        final BlockingQueue<JsonNode> inbox = new LinkedBlockingQueue<>();
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                inbox.add(JSON.readTree(buffer.toString()));
                buffer.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        JsonNode next() throws InterruptedException {
            JsonNode node = inbox.poll(5, TimeUnit.SECONDS);
            assertThat(node).as("5초 안에 메시지가 와야 한다").isNotNull();
            return node;
        }
    }

    private WebSocket connect(Client client) {
        return HttpClient.newHttpClient().newWebSocketBuilder()
                .header("Origin", "http://localhost:" + port)
                .buildAsync(URI.create("ws://localhost:" + port + "/ws/signaling"), client)
                .join();
    }

    private String createRoom() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/rooms"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"it\",\"maxParticipants\":2}"))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(201);
        return JSON.readTree(response.body()).get("code").toString().replace("\"", "");
    }

    @Test
    void twoClientsExchangeOfferThroughServer() throws Exception {
        String room = createRoom();
        Client alice = new Client();
        Client bob = new Client();
        WebSocket aliceSocket = connect(alice);
        WebSocket bobSocket = connect(bob);

        aliceSocket.sendText("{\"type\":\"join\",\"room\":\"" + room + "\",\"displayName\":\"Alice\"}", true).join();
        JsonNode aliceJoined = alice.next();
        assertThat(aliceJoined.get("type").toString()).isEqualTo("\"joined\"");

        bobSocket.sendText("{\"type\":\"join\",\"room\":\"" + room + "\",\"displayName\":\"Bob\"}", true).join();
        JsonNode bobJoined = bob.next();
        assertThat(bobJoined.get("peers").size()).isEqualTo(1);
        assertThat(alice.next().get("type").toString()).isEqualTo("\"peer-joined\"");

        String aliceId = bobJoined.get("peers").get(0).get("id").toString().replace("\"", "");
        bobSocket.sendText("{\"type\":\"offer\",\"to\":\"" + aliceId + "\",\"data\":{\"sdp\":\"v=0\"}}", true).join();

        JsonNode offer = alice.next();
        assertThat(offer.get("type").toString()).isEqualTo("\"offer\"");
        assertThat(offer.get("data").get("sdp").toString()).isEqualTo("\"v=0\"");

        bobSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye").join();
        assertThat(alice.next().get("type").toString()).isEqualTo("\"peer-left\"");
        aliceSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye").join();
    }

    @Test
    void joiningUnknownRoomReturnsError() throws Exception {
        Client client = new Client();
        WebSocket socket = connect(client);

        socket.sendText("{\"type\":\"join\",\"room\":\"zzz-zzzz-zzz\"}", true).join();

        JsonNode error = client.next();
        assertThat(error.get("code").toString()).isEqualTo("\"ROOM_NOT_FOUND\"");
        socket.sendClose(WebSocket.NORMAL_CLOSURE, "bye").join();
    }

    @Test
    void healthyApplicationServesStaticClient() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/index.html")).build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("meetlab");
    }
}
