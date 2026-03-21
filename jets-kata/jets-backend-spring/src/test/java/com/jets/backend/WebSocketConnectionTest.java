package com.jets.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
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

    @Test
    void shouldReceiveErrorWhenJoiningNonExistentLobby() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> errorMessage = new AtomicReference<>();

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketSession session = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"ERROR\"")) {
                    errorMessage.set(message.getPayload());
                    latch.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=Alice").get(5, TimeUnit.SECONDS);

        session.sendMessage(new TextMessage(
                "{\"type\":\"JOIN_LOBBY\",\"data\":{\"lobbyCode\":\"DOESNOTEXIST\"}}"));

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(errorMessage.get()).contains("\"type\":\"ERROR\"");
        assertThat(errorMessage.get()).contains("\"code\":\"LOBBY_NOT_FOUND\"");
    }

    @Test
    void shouldReceiveErrorWhenJoiningFullLobby() throws Exception {
        // Host erstellt Lobby
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
        }, "ws://localhost:" + port + "/ws/game?playerName=Host").get(5, TimeUnit.SECONDS);

        hostSession.sendMessage(new TextMessage("{\"type\":\"CREATE_LOBBY\",\"data\":{}}"));
        assertThat(lobbyCreatedLatch.await(5, TimeUnit.SECONDS)).isTrue();

        // 3 weitere Spieler joinen (Lobby damit voll mit 4)
        for (String name : List.of("Player2", "Player3", "Player4")) {
            CountDownLatch joinedLatch = new CountDownLatch(1);
            WebSocketSession s = client.execute(new TextWebSocketHandler() {
                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                    if (message.getPayload().contains("\"type\":\"LOBBY_STATE\"")) {
                        joinedLatch.countDown();
                    }
                }
            }, "ws://localhost:" + port + "/ws/game?playerName=" + name).get(5, TimeUnit.SECONDS);
            s.sendMessage(new TextMessage(
                    "{\"type\":\"JOIN_LOBBY\",\"data\":{\"lobbyCode\":\"" + lobbyCode.get() + "\"}}"));
            assertThat(joinedLatch.await(5, TimeUnit.SECONDS)).isTrue();
        }

        // 5. Spieler versucht beizutreten
        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<String> errorMessage = new AtomicReference<>();

        WebSocketSession fifthPlayer = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"ERROR\"")) {
                    errorMessage.set(message.getPayload());
                    errorLatch.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=Player5").get(5, TimeUnit.SECONDS);

        fifthPlayer.sendMessage(new TextMessage(
                "{\"type\":\"JOIN_LOBBY\",\"data\":{\"lobbyCode\":\"" + lobbyCode.get() + "\"}}"));

        assertThat(errorLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(errorMessage.get()).contains("\"type\":\"ERROR\"");
        assertThat(errorMessage.get()).contains("\"code\":\"LOBBY_FULL\"");
    }

    @Test
    void shouldReceiveErrorWhenJoiningLobbyWithGameInProgress() throws Exception {
        AtomicReference<String> lobbyCode = new AtomicReference<>();
        CountDownLatch lobbyCreatedLatch = new CountDownLatch(1);
        CountDownLatch gameStartingLatch = new CountDownLatch(1);

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketSession hostSession = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"LOBBY_CREATED\"")) {
                    lobbyCode.set(message.getPayload().replaceAll(".*\"lobbyCode\":\"([^\"]+)\".*", "$1"));
                    lobbyCreatedLatch.countDown();
                } else if (message.getPayload().contains("\"type\":\"GAME_STARTING\"")) {
                    gameStartingLatch.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=Host").get(5, TimeUnit.SECONDS);

        hostSession.sendMessage(new TextMessage("{\"type\":\"CREATE_LOBBY\",\"data\":{}}"));
        assertThat(lobbyCreatedLatch.await(5, TimeUnit.SECONDS)).isTrue();

        // Zweiter Spieler joined
        CountDownLatch joinedLatch = new CountDownLatch(1);
        WebSocketSession player2 = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"LOBBY_STATE\"")) {
                    joinedLatch.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=Player2").get(5, TimeUnit.SECONDS);

        player2.sendMessage(new TextMessage(
                "{\"type\":\"JOIN_LOBBY\",\"data\":{\"lobbyCode\":\"" + lobbyCode.get() + "\"}}"));
        assertThat(joinedLatch.await(5, TimeUnit.SECONDS)).isTrue();

        // Host startet das Spiel und wartet auf GAME_STARTING
        hostSession.sendMessage(new TextMessage("{\"type\":\"START_GAME\",\"data\":{}}"));
        assertThat(gameStartingLatch.await(5, TimeUnit.SECONDS)).isTrue();

        // Neuer Spieler versucht beizutreten während Spiel läuft
        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<String> errorMessage = new AtomicReference<>();

        WebSocketSession latePlayer = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"ERROR\"")) {
                    errorMessage.set(message.getPayload());
                    errorLatch.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=LatePlayer").get(5, TimeUnit.SECONDS);

        latePlayer.sendMessage(new TextMessage(
                "{\"type\":\"JOIN_LOBBY\",\"data\":{\"lobbyCode\":\"" + lobbyCode.get() + "\"}}"));

        assertThat(errorLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(errorMessage.get()).contains("\"type\":\"ERROR\"");
        assertThat(errorMessage.get()).contains("\"code\":\"GAME_IN_PROGRESS\"");
    }

    @Test
    void shouldReceiveErrorWhenNonHostSendsStartGame() throws Exception {
        // Host erstellt Lobby
        AtomicReference<String> lobbyCode = new AtomicReference<>();
        CountDownLatch lobbyCreatedLatch = new CountDownLatch(1);

        StandardWebSocketClient client = new StandardWebSocketClient();
        client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"LOBBY_CREATED\"")) {
                    lobbyCode.set(message.getPayload().replaceAll(".*\"lobbyCode\":\"([^\"]+)\".*", "$1"));
                    lobbyCreatedLatch.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=Host").get(5, TimeUnit.SECONDS)
                .sendMessage(new TextMessage("{\"type\":\"CREATE_LOBBY\",\"data\":{}}"));

        assertThat(lobbyCreatedLatch.await(5, TimeUnit.SECONDS)).isTrue();

        // Player2 joined
        CountDownLatch joinedLatch = new CountDownLatch(1);
        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<String> errorMessage = new AtomicReference<>();

        WebSocketSession player2 = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"LOBBY_STATE\"")) {
                    joinedLatch.countDown();
                } else if (message.getPayload().contains("\"type\":\"ERROR\"")) {
                    errorMessage.set(message.getPayload());
                    errorLatch.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=Player2").get(5, TimeUnit.SECONDS);

        player2.sendMessage(new TextMessage(
                "{\"type\":\"JOIN_LOBBY\",\"data\":{\"lobbyCode\":\"" + lobbyCode.get() + "\"}}"));
        assertThat(joinedLatch.await(5, TimeUnit.SECONDS)).isTrue();

        // Player2 (kein Host) versucht START_GAME zu senden
        player2.sendMessage(new TextMessage("{\"type\":\"START_GAME\",\"data\":{}}"));

        assertThat(errorLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(errorMessage.get()).contains("\"type\":\"ERROR\"");
        assertThat(errorMessage.get()).contains("\"code\":\"NOT_HOST\"");
    }

    @Test
    void shouldReceiveLobbyStateWithPlayersListAfterJoin() throws Exception {
        AtomicReference<String> lobbyCode = new AtomicReference<>();
        CountDownLatch lobbyCreatedLatch = new CountDownLatch(1);

        StandardWebSocketClient client = new StandardWebSocketClient();
        client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"LOBBY_CREATED\"")) {
                    lobbyCode.set(message.getPayload().replaceAll(".*\"lobbyCode\":\"([^\"]+)\".*", "$1"));
                    lobbyCreatedLatch.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=Alice").get(5, TimeUnit.SECONDS)
                .sendMessage(new TextMessage("{\"type\":\"CREATE_LOBBY\",\"data\":{}}"));

        assertThat(lobbyCreatedLatch.await(5, TimeUnit.SECONDS)).isTrue();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> lobbyStateMessage = new AtomicReference<>();

        WebSocketSession bob = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"LOBBY_STATE\"")) {
                    lobbyStateMessage.set(message.getPayload());
                    latch.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=Bob").get(5, TimeUnit.SECONDS);

        bob.sendMessage(new TextMessage(
                "{\"type\":\"JOIN_LOBBY\",\"data\":{\"lobbyCode\":\"" + lobbyCode.get() + "\"}}"));

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(lobbyStateMessage.get()).contains("\"players\"");
        assertThat(lobbyStateMessage.get()).contains("\"name\":\"Alice\"");
        assertThat(lobbyStateMessage.get()).contains("\"name\":\"Bob\"");
    }

    @Test
    void shouldReceiveUpdatedLobbyStateAfterPlayerReady() throws Exception {
        AtomicReference<String> lobbyCode = new AtomicReference<>();
        CountDownLatch lobbyCreatedLatch = new CountDownLatch(1);
        CountDownLatch joinedLatch = new CountDownLatch(1);
        CountDownLatch readyLatch = new CountDownLatch(1);
        AtomicReference<String> readyMessage = new AtomicReference<>();

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketSession alice = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"LOBBY_CREATED\"")) {
                    lobbyCode.set(message.getPayload().replaceAll(".*\"lobbyCode\":\"([^\"]+)\".*", "$1"));
                    lobbyCreatedLatch.countDown();
                } else if (message.getPayload().contains("\"type\":\"LOBBY_STATE\"")
                        && message.getPayload().contains("\"ready\":true")) {
                    readyMessage.set(message.getPayload());
                    readyLatch.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=Alice").get(5, TimeUnit.SECONDS);

        alice.sendMessage(new TextMessage("{\"type\":\"CREATE_LOBBY\",\"data\":{}}"));
        assertThat(lobbyCreatedLatch.await(5, TimeUnit.SECONDS)).isTrue();

        WebSocketSession bob = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"LOBBY_STATE\"")) {
                    joinedLatch.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=Bob").get(5, TimeUnit.SECONDS);

        bob.sendMessage(new TextMessage(
                "{\"type\":\"JOIN_LOBBY\",\"data\":{\"lobbyCode\":\"" + lobbyCode.get() + "\"}}"));
        assertThat(joinedLatch.await(5, TimeUnit.SECONDS)).isTrue();

        alice.sendMessage(new TextMessage("{\"type\":\"PLAYER_READY\",\"data\":{\"ready\":true}}"));

        assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(readyMessage.get()).contains("\"ready\":true");
    }

    @Test
    void shouldReceiveUpdatedLobbyStateAfterSetGameMode() throws Exception {
        AtomicReference<String> lobbyCode = new AtomicReference<>();
        CountDownLatch lobbyCreatedLatch = new CountDownLatch(1);
        CountDownLatch gameModeLatch = new CountDownLatch(1);
        AtomicReference<String> gameModeMessage = new AtomicReference<>();

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketSession alice = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"LOBBY_CREATED\"")) {
                    lobbyCode.set(message.getPayload().replaceAll(".*\"lobbyCode\":\"([^\"]+)\".*", "$1"));
                    lobbyCreatedLatch.countDown();
                } else if (message.getPayload().contains("\"type\":\"LOBBY_STATE\"")
                        && message.getPayload().contains("\"gameMode\"")) {
                    gameModeMessage.set(message.getPayload());
                    gameModeLatch.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=Alice").get(5, TimeUnit.SECONDS);

        alice.sendMessage(new TextMessage("{\"type\":\"CREATE_LOBBY\",\"data\":{}}"));
        assertThat(lobbyCreatedLatch.await(5, TimeUnit.SECONDS)).isTrue();

        alice.sendMessage(new TextMessage("{\"type\":\"SET_GAME_MODE\",\"data\":{\"gameMode\":\"FFA\"}}"));

        assertThat(gameModeLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(gameModeMessage.get()).contains("\"gameMode\":\"FFA\"");
    }

    @Test
    void shouldReceiveLobbyStateAfterPlayerLeaves() throws Exception {
        AtomicReference<String> lobbyCode = new AtomicReference<>();
        CountDownLatch lobbyCreatedLatch = new CountDownLatch(1);
        CountDownLatch joinedLatch = new CountDownLatch(1);
        CountDownLatch leftLatch = new CountDownLatch(1);
        AtomicReference<String> leftMessage = new AtomicReference<>();

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketSession alice = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"LOBBY_CREATED\"")) {
                    lobbyCode.set(message.getPayload().replaceAll(".*\"lobbyCode\":\"([^\"]+)\".*", "$1"));
                    lobbyCreatedLatch.countDown();
                } else if (message.getPayload().contains("\"type\":\"LOBBY_STATE\"")
                        && !message.getPayload().contains("\"name\":\"Bob\"")) {
                    leftMessage.set(message.getPayload());
                    leftLatch.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=Alice").get(5, TimeUnit.SECONDS);

        alice.sendMessage(new TextMessage("{\"type\":\"CREATE_LOBBY\",\"data\":{}}"));
        assertThat(lobbyCreatedLatch.await(5, TimeUnit.SECONDS)).isTrue();

        WebSocketSession bob = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (message.getPayload().contains("\"type\":\"LOBBY_STATE\"")) {
                    joinedLatch.countDown();
                }
            }
        }, "ws://localhost:" + port + "/ws/game?playerName=Bob").get(5, TimeUnit.SECONDS);

        bob.sendMessage(new TextMessage(
                "{\"type\":\"JOIN_LOBBY\",\"data\":{\"lobbyCode\":\"" + lobbyCode.get() + "\"}}"));
        assertThat(joinedLatch.await(5, TimeUnit.SECONDS)).isTrue();

        bob.sendMessage(new TextMessage("{\"type\":\"LEAVE_LOBBY\",\"data\":{}}"));

        assertThat(leftLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(leftMessage.get()).contains("\"type\":\"LOBBY_STATE\"");
        assertThat(leftMessage.get()).doesNotContain("\"name\":\"Bob\"");
    }
}