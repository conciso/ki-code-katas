package com.jets.backend;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.UUID;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final LobbyService lobbyService;

    public GameWebSocketHandler(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        lobbyService.registerSession(session);
        String playerId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        session.getAttributes().put("playerId", playerId);
        String playerName = extractPlayerName(session);
        session.getAttributes().put("playerName", playerName);
        String message = """
                {"type":"CONNECTED","data":{"playerId":"%s","serverTickRate":30}}
                """.formatted(playerId).strip();
        session.sendMessage(new TextMessage(message));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        if (payload.contains("\"type\":\"CREATE_LOBBY\"")) {
            String lobbyCode = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
            String hostId = (String) session.getAttributes().get("playerId");
            String hostName = (String) session.getAttributes().get("playerName");
            lobbyService.createLobby(lobbyCode, hostId, hostName, session);
            session.sendMessage(new TextMessage(
                    "{\"type\":\"LOBBY_CREATED\",\"data\":{\"lobbyCode\":\"%s\",\"hostId\":\"%s\"}}"
                            .formatted(lobbyCode, hostId)));

        } else if (payload.contains("\"type\":\"JOIN_LOBBY\"")) {
            String lobbyCode = payload.replaceAll(".*\"lobbyCode\":\"([^\"]+)\".*", "$1");
            if (!lobbyService.lobbyExists(lobbyCode)) {
                session.sendMessage(new TextMessage(
                        "{\"type\":\"ERROR\",\"data\":{\"code\":\"LOBBY_NOT_FOUND\",\"message\":\"Lobby nicht gefunden\"}}"));
                return;
            }
            if (lobbyService.gameInProgress(lobbyCode)) {
                session.sendMessage(new TextMessage(
                        "{\"type\":\"ERROR\",\"data\":{\"code\":\"GAME_IN_PROGRESS\",\"message\":\"Das Spiel hat bereits begonnen\"}}"));
                return;
            }
            if (lobbyService.lobbyFull(lobbyCode)) {
                session.sendMessage(new TextMessage(
                        "{\"type\":\"ERROR\",\"data\":{\"code\":\"LOBBY_FULL\",\"message\":\"Die Lobby ist voll (max. 4 Spieler)\"}}"));
                return;
            }
            String playerId = (String) session.getAttributes().get("playerId");
            String playerName = (String) session.getAttributes().get("playerName");
            lobbyService.joinLobby(lobbyCode, playerId, playerName, session);
            broadcast(lobbyCode);

        } else if (payload.contains("\"type\":\"LEAVE_LOBBY\"")) {
            String lobbyCode = lobbyService.getLobbyCodeForSession(session);
            if (lobbyCode != null) {
                lobbyService.removePlayer(lobbyCode, session);
                broadcast(lobbyCode);
            }

        } else if (payload.contains("\"type\":\"PLAYER_READY\"")) {
            String lobbyCode = lobbyService.getLobbyCodeForSession(session);
            boolean ready = payload.contains("\"ready\":true");
            lobbyService.setReady(lobbyCode, session, ready);
            broadcast(lobbyCode);

        } else if (payload.contains("\"type\":\"SET_GAME_MODE\"")) {
            String lobbyCode = lobbyService.getLobbyCodeForSession(session);
            String gameMode = payload.replaceAll(".*\"gameMode\":\"([^\"]+)\".*", "$1");
            lobbyService.setGameMode(lobbyCode, gameMode);
            broadcast(lobbyCode);

        } else if (payload.contains("\"type\":\"START_GAME\"")) {
            String lobbyCode = lobbyService.getLobbyCodeForSession(session);
            if (!lobbyService.isHost(lobbyCode, session)) {
                session.sendMessage(new TextMessage(
                        "{\"type\":\"ERROR\",\"data\":{\"code\":\"NOT_HOST\",\"message\":\"Nur der Host darf das Spiel starten\"}}"));
                return;
            }
            lobbyService.startGame(lobbyCode);
            String response = "{\"type\":\"GAME_STARTING\",\"data\":{}}";
            for (WebSocketSession s : lobbyService.getSessions(lobbyCode)) {
                s.sendMessage(new TextMessage(response));
            }

        } else if (payload.contains("\"type\":\"PING\"")) {
            long timestamp = Long.parseLong(payload.replaceAll(".*\"timestamp\":(\\d+).*", "$1"));
            session.sendMessage(new TextMessage(
                    "{\"type\":\"PONG\",\"data\":{\"timestamp\":%d}}".formatted(timestamp)));
        }
    }

    private void broadcast(String lobbyCode) throws Exception {
        String lobbyState = lobbyService.buildLobbyState(lobbyCode);
        for (WebSocketSession s : lobbyService.getSessions(lobbyCode)) {
            s.sendMessage(new TextMessage(lobbyState));
        }
    }

    private String extractPlayerName(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : "";
        if (query != null && query.contains("playerName=")) {
            return query.replaceAll(".*playerName=([^&]*).*", "$1");
        }
        return "Unknown";
    }
}
