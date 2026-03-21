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
class StartGameErrorTest {

    @LocalServerPort
    int port;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private WebSocketTestSupport support;

    @Test
    void shouldReturnErrorWhenNonHostStartsGame() throws Exception {
        support = new WebSocketTestSupport(objectMapper, port);
        var aliceMessages = new ArrayBlockingQueue<String>(20);
        var bobMessages   = new ArrayBlockingQueue<String>(20);

        WebSocketSession aliceSession = support.connect("Alice", aliceMessages);
        support.receiveConnected(aliceMessages);
        String lobbyCode = support.createLobby(aliceSession, aliceMessages);

        WebSocketSession bobSession = support.connect("Bob", bobMessages);
        support.receiveConnected(bobMessages);
        support.sendJoinLobby(bobSession, lobbyCode);
        support.receiveLobbyState(bobMessages);
        support.receiveLobbyState(aliceMessages);

        support.sendPlayerReady(aliceSession, true);
        support.receiveLobbyState(aliceMessages);
        support.receiveLobbyState(bobMessages);

        support.sendPlayerReady(bobSession, true);
        support.receiveLobbyState(aliceMessages);
        support.receiveLobbyState(bobMessages);

        support.sendStartGame(bobSession);

        JsonNode error = support.receiveError(bobMessages);
        assertThat(error.get("code").stringValue()).isEqualTo("NOT_HOST");
    }

    @Test
    void shouldReturnErrorWhenNotAllPlayersAreReady() throws Exception {
        support = new WebSocketTestSupport(objectMapper, port);
        var aliceMessages = new ArrayBlockingQueue<String>(20);
        var bobMessages   = new ArrayBlockingQueue<String>(20);

        WebSocketSession aliceSession = support.connect("Alice", aliceMessages);
        support.receiveConnected(aliceMessages);
        String lobbyCode = support.createLobby(aliceSession, aliceMessages);

        WebSocketSession bobSession = support.connect("Bob", bobMessages);
        support.receiveConnected(bobMessages);
        support.sendJoinLobby(bobSession, lobbyCode);
        support.receiveLobbyState(bobMessages);
        support.receiveLobbyState(aliceMessages);

        support.sendPlayerReady(aliceSession, true);
        support.receiveLobbyState(aliceMessages);
        support.receiveLobbyState(bobMessages);

        support.sendStartGame(aliceSession);

        JsonNode error = support.receiveError(aliceMessages);
        assertThat(error.get("code").stringValue()).isEqualTo("NOT_ALL_READY");
    }

    @Test
    void shouldReturnErrorWhenOnlyOnePlayerInLobby() throws Exception {
        support = new WebSocketTestSupport(objectMapper, port);
        var aliceMessages = new ArrayBlockingQueue<String>(20);

        WebSocketSession aliceSession = support.connect("Alice", aliceMessages);
        support.receiveConnected(aliceMessages);
        support.createLobby(aliceSession, aliceMessages);

        support.sendPlayerReady(aliceSession, true);
        support.receiveLobbyState(aliceMessages);

        support.sendStartGame(aliceSession);

        JsonNode error = support.receiveError(aliceMessages);
        assertThat(error.get("code").stringValue()).isEqualTo("NOT_ENOUGH_PLAYERS");
    }

    @Test
    void shouldReturnErrorWhenTryingToJoinLobbyWithGameInProgress() throws Exception {
        support = new WebSocketTestSupport(objectMapper, port);
        var aliceMessages = new ArrayBlockingQueue<String>(20);
        var bobMessages   = new ArrayBlockingQueue<String>(20);
        var charlieMessages = new ArrayBlockingQueue<String>(20);

        WebSocketSession aliceSession = support.connect("Alice", aliceMessages);
        support.receiveConnected(aliceMessages);
        String lobbyCode = support.createLobby(aliceSession, aliceMessages);

        WebSocketSession bobSession = support.connect("Bob", bobMessages);
        support.receiveConnected(bobMessages);
        support.sendJoinLobby(bobSession, lobbyCode);
        support.receiveLobbyState(bobMessages);
        support.receiveLobbyState(aliceMessages);

        support.sendPlayerReady(aliceSession, true);
        support.receiveLobbyState(aliceMessages);
        support.receiveLobbyState(bobMessages);

        support.sendPlayerReady(bobSession, true);
        support.receiveLobbyState(aliceMessages);
        support.receiveLobbyState(bobMessages);

        support.sendStartGame(aliceSession);
        support.receiveGameStarting(aliceMessages);
        support.receiveGameStarting(bobMessages);

        WebSocketSession charlieSession = support.connect("Charlie", charlieMessages);
        support.receiveConnected(charlieMessages);
        support.sendJoinLobby(charlieSession, lobbyCode);

        JsonNode error = support.receiveError(charlieMessages);
        assertThat(error.get("code").stringValue()).isEqualTo("GAME_IN_PROGRESS");
    }
}

