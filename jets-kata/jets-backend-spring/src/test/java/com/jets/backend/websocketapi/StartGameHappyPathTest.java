package com.jets.backend.websocketapi;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ArrayBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StartGameHappyPathTest {

    @LocalServerPort
    int port;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private WebSocketTestSupport support;

    @Test
    void shouldStartGameAndBroadcastGameStartingWhenHostAndAllPlayersReady() throws Exception {
        support = new WebSocketTestSupport(objectMapper, port);
        var aliceMessages = new ArrayBlockingQueue<String>(20);
        var bobMessages   = new ArrayBlockingQueue<String>(20);

        // --- Alice verbindet und erstellt eine Lobby ---
        WebSocketSession aliceSession = support.connect("Alice", aliceMessages);
        String aliceId = support.receiveConnected(aliceMessages);
        String lobbyCode = support.createLobby(aliceSession, aliceMessages);

        // --- Bob verbindet und tritt bei ---
        WebSocketSession bobSession = support.connect("Bob", bobMessages);
        String bobId = support.receiveConnected(bobMessages);
        support.sendJoinLobby(bobSession, lobbyCode);
        support.receiveLobbyState(bobMessages); // Bob empfängt LOBBY_STATE
        support.receiveLobbyState(aliceMessages); // Alice empfängt LOBBY_STATE

        // --- Beide Spieler signalisieren Bereitschaft ---
        support.sendPlayerReady(aliceSession, true);
        support.receiveLobbyState(aliceMessages); // Alice: LOBBY_STATE mit ready=true
        support.receiveLobbyState(bobMessages);   // Bob: LOBBY_STATE mit ready=true

        support.sendPlayerReady(bobSession, true);
        support.receiveLobbyState(aliceMessages); // Alice: LOBBY_STATE mit ready=true
        support.receiveLobbyState(bobMessages);   // Bob: LOBBY_STATE mit ready=true

        // --- Host (Alice) startet das Spiel ---
        support.sendStartGame(aliceSession);

        // --- GAME_STARTING sollte an beide Spieler gesendet werden ---
        JsonNode aliceGameStarting = support.receiveGameStarting(aliceMessages);
        JsonNode bobGameStarting = support.receiveGameStarting(bobMessages);

        assertThat(aliceGameStarting.get("countdown").intValue()).isGreaterThan(0);
        assertThat(aliceGameStarting.get("gameMode").stringValue()).isEqualTo("COOP");
        assertThat(aliceGameStarting.get("fieldWidth").intValue()).isGreaterThan(0);
        assertThat(aliceGameStarting.get("fieldHeight").intValue()).isGreaterThan(0);

        // --- Spieler sollten spawn-Positionen haben ---
        assertThat(aliceGameStarting.get("players").isArray()).isTrue();
        assertThat(aliceGameStarting.get("players").size()).isEqualTo(2);

        JsonNode alicePlayer = support.findPlayerInGameStarting(aliceGameStarting, aliceId);
        assertThat(alicePlayer.get("name").stringValue()).isEqualTo("Alice");
        assertThat(alicePlayer.has("spawnX")).isTrue();
        assertThat(alicePlayer.has("spawnY")).isTrue();

        JsonNode bobPlayer = support.findPlayerInGameStarting(aliceGameStarting, bobId);
        assertThat(bobPlayer.get("name").stringValue()).isEqualTo("Bob");
    }
}


