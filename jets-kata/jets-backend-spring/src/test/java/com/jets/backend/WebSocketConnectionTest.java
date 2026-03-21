package com.jets.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketConnectionTest {

    @LocalServerPort
    private int port;

    @Test
    void shouldReceiveConnectedMessageAfterConnect() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();

        StandardWebSocketClient client = new StandardWebSocketClient();
        client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                receivedMessage.set(message.getPayload());
                latch.countDown();
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=TestPlayer");

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedMessage.get()).contains("\"type\":\"CONNECTED\"");
        assertThat(receivedMessage.get()).contains("\"playerId\"");
        assertThat(receivedMessage.get()).contains("\"serverTickRate\":30");
    }

    @Test
    void shouldReceiveLobbyCreatedAfterCreateLobby() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> lobbyCreatedMessage = new AtomicReference<>();

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketSession session = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"LOBBY_CREATED\"")) {
                    lobbyCreatedMessage.set(message.getPayload());
                    latch.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=Alice").get(5, TimeUnit.SECONDS);

        session.sendMessage(new TextMessage("{\"type\":\"CREATE_LOBBY\",\"data\":{}}"));

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(lobbyCreatedMessage.get()).contains("\"type\":\"LOBBY_CREATED\"");
        assertThat(lobbyCreatedMessage.get()).contains("\"lobbyCode\"");
        assertThat(lobbyCreatedMessage.get()).contains("\"hostId\"");
    }

    @Test
    void shouldReceivePongWithSameTimestampAfterPing() throws Exception {
        long timestamp = 1710950400000L;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> pongMessage = new AtomicReference<>();

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketSession session = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"PONG\"")) {
                    pongMessage.set(message.getPayload());
                    latch.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=TestPlayer").get(5, TimeUnit.SECONDS);

        session.sendMessage(new TextMessage("{\"type\":\"PING\",\"data\":{\"timestamp\":" + timestamp + "}}"));

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(pongMessage.get()).contains("\"type\":\"PONG\"");
        assertThat(pongMessage.get()).contains("\"timestamp\":" + timestamp);
    }

    @Test
    void shouldReceiveLobbyStateAfterJoiningExistingLobby() throws Exception {
        // Client A erstellt eine Lobby und liest den lobbyCode
        AtomicReference<String> lobbyCode = new AtomicReference<>();
        CountDownLatch lobbyCreatedLatch = new CountDownLatch(1);

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketSession hostSession = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"LOBBY_CREATED\"")) {
                    lobbyCode.set(message.getPayload().replaceAll(".*\"lobbyCode\":\"([^\"]+)\".*", "$1"));
                    lobbyCreatedLatch.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=Alice").get(5, TimeUnit.SECONDS);

        hostSession.sendMessage(new TextMessage("{\"type\":\"CREATE_LOBBY\",\"data\":{}}"));
        assertThat(lobbyCreatedLatch.await(5, TimeUnit.SECONDS)).isTrue();

        // Client B tritt der Lobby bei
        CountDownLatch lobbyStateLatch = new CountDownLatch(1);
        AtomicReference<String> lobbyStateMessage = new AtomicReference<>();

        WebSocketSession joinerSession = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"LOBBY_STATE\"")) {
                    lobbyStateMessage.set(message.getPayload());
                    lobbyStateLatch.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=Bob").get(5, TimeUnit.SECONDS);

        joinerSession.sendMessage(new TextMessage(
                "{\"type\":\"JOIN_LOBBY\",\"data\":{\"lobbyCode\":\"" + lobbyCode.get() + "\"}}"));

        assertThat(lobbyStateLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(lobbyStateMessage.get()).contains("\"type\":\"LOBBY_STATE\"");
        assertThat(lobbyStateMessage.get()).contains("\"lobbyCode\":\"" + lobbyCode.get() + "\"");
    }

    @Test
    void shouldReceiveGameStartingAfterHostSendsStartGame() throws Exception {
        // Client A erstellt Lobby
        AtomicReference<String> lobbyCode = new AtomicReference<>();
        CountDownLatch lobbyCreatedLatch = new CountDownLatch(1);
        CountDownLatch gameStartingLatch = new CountDownLatch(1);
        AtomicReference<String> gameStartingMessage = new AtomicReference<>();

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketSession hostSession = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"LOBBY_CREATED\"")) {
                    lobbyCode.set(message.getPayload().replaceAll(".*\"lobbyCode\":\"([^\"]+)\".*", "$1"));
                    lobbyCreatedLatch.countDown();
                } else if (message.getPayload().contains("\"type\":\"GAME_STARTING\"")) {
                    gameStartingMessage.set(message.getPayload());
                    gameStartingLatch.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=Alice").get(5, TimeUnit.SECONDS);

        hostSession.sendMessage(new TextMessage("{\"type\":\"CREATE_LOBBY\",\"data\":{}}"));
        assertThat(lobbyCreatedLatch.await(5, TimeUnit.SECONDS)).isTrue();

        // Client B tritt bei und wartet auf LOBBY_STATE
        CountDownLatch joinedLatch = new CountDownLatch(1);
        WebSocketSession joinerSession = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"LOBBY_STATE\"")) {
                    joinedLatch.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=Bob").get(5, TimeUnit.SECONDS);

        joinerSession.sendMessage(new TextMessage(
                "{\"type\":\"JOIN_LOBBY\",\"data\":{\"lobbyCode\":\"" + lobbyCode.get() + "\"}}"));
        assertThat(joinedLatch.await(5, TimeUnit.SECONDS)).isTrue();

        // Host startet das Spiel
        hostSession.sendMessage(new TextMessage("{\"type\":\"START_GAME\",\"data\":{}}"));

        assertThat(gameStartingLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(gameStartingMessage.get()).contains("\"type\":\"GAME_STARTING\"");
    }

    @Test
    void shouldBroadcastLobbyStateToAllPlayersWhenSomeoneJoins() throws Exception {
        AtomicReference<String> lobbyCode = new AtomicReference<>();
        CountDownLatch lobbyCreatedLatch = new CountDownLatch(1);
        CountDownLatch hostReceivedLobbyState = new CountDownLatch(1);

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketSession hostSession = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"LOBBY_CREATED\"")) {
                    lobbyCode.set(message.getPayload().replaceAll(".*\"lobbyCode\":\"([^\"]+)\".*", "$1"));
                    lobbyCreatedLatch.countDown();
                } else if (message.getPayload().contains("\"type\":\"LOBBY_STATE\"")) {
                    hostReceivedLobbyState.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=Alice").get(5, TimeUnit.SECONDS);

        hostSession.sendMessage(new TextMessage("{\"type\":\"CREATE_LOBBY\",\"data\":{}}"));
        assertThat(lobbyCreatedLatch.await(5, TimeUnit.SECONDS)).isTrue();

        // Bob tritt bei
        CountDownLatch bobReceivedLobbyState = new CountDownLatch(1);
        WebSocketSession joinerSession = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"LOBBY_STATE\"")) {
                    bobReceivedLobbyState.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=Bob").get(5, TimeUnit.SECONDS);

        joinerSession.sendMessage(new TextMessage(
                "{\"type\":\"JOIN_LOBBY\",\"data\":{\"lobbyCode\":\"" + lobbyCode.get() + "\"}}"));

        assertThat(bobReceivedLobbyState.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(hostReceivedLobbyState.await(5, TimeUnit.SECONDS)).isTrue();
    }
}