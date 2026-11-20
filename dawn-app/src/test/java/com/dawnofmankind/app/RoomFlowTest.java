package com.dawnofmankind.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

/** The whole app: create a room over HTTP, join, connect two players over WebSocket, start and play. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RoomFlowTest {
    @Autowired
    Environment env;

    final HttpClient http = HttpClient.newHttpClient();
    final JsonMapper json = JsonMapper.builder().build();

    String base() {
        return "http://localhost:" + env.getProperty("local.server.port");
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> post(String path, Map<String, Object> body) throws Exception {
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(base() + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, r.statusCode(), r.body());
        return json.readValue(r.body(), Map.class);
    }

    /** A player's socket: collects every message from the server. */
    final class Client implements WebSocket.Listener {
        final BlockingQueue<Map<String, Object>> inbox = new LinkedBlockingQueue<>();
        final StringBuilder part = new StringBuilder();
        WebSocket ws;

        Client(String code, String token) throws Exception {
            String url = base().replace("http", "ws") + "/dawn/ws?code=" + code + "&token=" + token;
            ws = http.newWebSocketBuilder().header("Origin", base()).buildAsync(URI.create(url), this).get(5, TimeUnit.SECONDS);
        }

        @Override
        @SuppressWarnings("unchecked")
        public CompletionStage<?> onText(WebSocket w, CharSequence data, boolean last) {
            part.append(data);
            if (last) {
                inbox.add(json.readValue(part.toString(), Map.class));
                part.setLength(0);
            }
            w.request(1);
            return null;
        }

        void send(Map<String, Object> m) throws Exception {
            ws.sendText(json.writeValueAsString(m), true).get(5, TimeUnit.SECONDS);
        }

        /** Waits for a state message that matches. */
        @SuppressWarnings("unchecked")
        Map<String, Object> await(java.util.function.Predicate<Map<String, Object>> p) throws Exception {
            long end = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < end) {
                Map<String, Object> m = inbox.poll(200, TimeUnit.MILLISECONDS);
                if (m != null && p.test(m)) return m;
            }
            throw new AssertionError("no matching message");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void createJoinStartAndPlay() throws Exception {
        Map<String, Object> a = post("/dawn/api/rooms", Map.of("name", "Ada"));
        String code = (String) a.get("code");
        assertEquals(5, code.length());
        Map<String, Object> b = post("/dawn/api/rooms/" + code.toLowerCase() + "/join", Map.of("name", "Ada"));
        assertEquals(1, b.get("seat"));

        Client ca = new Client(code, (String) a.get("token"));
        Client cb = new Client(code, (String) b.get("token"));
        Map<String, Object> lobby = cb.await(m -> "state".equals(m.get("kind"))
                && ((java.util.List<?>) ((Map<String, Object>) m.get("room")).get("players")).size() == 2);
        var players = (java.util.List<Map<String, Object>>) ((Map<String, Object>) lobby.get("room")).get("players");
        assertEquals("Ada 2", players.get(1).get("name"), "duplicate names get a number");

        cb.send(Map.of("type", "start"));
        cb.await(m -> "error".equals(m.get("kind")));

        ca.send(Map.of("type", "start"));
        Map<String, Object> st = ca.await(m -> m.get("game") != null);
        Map<String, Object> game = (Map<String, Object>) st.get("game");
        assertEquals("turns", game.get("phase"));
        int current = (Integer) game.get("currentPlayer");
        Client mover = current == 0 ? ca : cb;
        mover.send(Map.of("type", "recruit"));
        Map<String, Object> after = mover.await(m -> m.get("game") != null && (Integer) ((Map<String, Object>) m.get("game")).get("actionsLeft") == 3);
        Map<String, Object> g2 = (Map<String, Object>) after.get("game");
        Map<String, Object> mine = ((java.util.List<Map<String, Object>>) g2.get("players")).get(current);
        assertEquals(1, mine.get("army"));
        assertTrue(mine.containsKey("hand"), "you see your own hand");
        Map<String, Object> theirs = ((java.util.List<Map<String, Object>>) g2.get("players")).get(1 - current);
        assertTrue(!theirs.containsKey("hand"), "you don't see the other hand");

        // chat reaches everyone
        ca.send(Map.of("type", "chat", "text", "hello"));
        cb.await(m -> !((java.util.List<?>) m.get("chat")).isEmpty());
    }

    @Test
    void badTokenIsTurnedAway() throws Exception {
        Map<String, Object> a = post("/dawn/api/rooms", Map.of("name", "Bo"));
        Client c = new Client((String) a.get("code"), "nope");
        Map<String, Object> m = c.await(x -> "gone".equals(x.get("kind")));
        assertTrue(String.valueOf(m.get("message")).contains("not a player"));
    }

    @Test
    void pageIsServed() throws Exception {
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(base() + "/dawn/")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("Dawn of Mankind"));
        HttpResponse<String> js = http.send(HttpRequest.newBuilder(URI.create(base() + "/dawn/app.js")).timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, js.statusCode());
    }
}
