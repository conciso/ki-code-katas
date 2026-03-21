package com.jets.backend;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.UUID;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final LobbyService lobbyService;
    private final GameService gameService;

    public GameWebSocketHandler(LobbyService lobbyService, GameService gameService) {
        this.lobbyService = lobbyService;
        this.gameService = gameService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        lobbyService.registerSession(session);
        String playerId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        session.getAttributes().put("playerId", playerId);
        session.getAttributes().put("playerName", extractPlayerName(session));
        session.sendMessage(new TextMessage(
                "{\"type\":\"CONNECTED\",\"data\":{\"playerId\":\"%s\",\"serverTickRate\":30}}".formatted(playerId)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        switch (MessageType.from(payload)) {
            case CREATE_LOBBY  -> handleCreateLobby(session);
            case JOIN_LOBBY    -> handleJoinLobby(session, payload);
            case LEAVE_LOBBY   -> handleLeaveLobby(session);
            case PLAYER_READY  -> handlePlayerReady(session, payload);
            case SET_GAME_MODE -> handleSetGameMode(session, payload);
            case START_GAME    -> handleStartGame(session);
            case PLAYER_INPUT  -> handlePlayerInput(session, payload);
            case PING          -> handlePing(session, payload);
            default            -> { }
        }
    }

    private void handleCreateLobby(WebSocketSession session) throws Exception {
        String lobbyCode = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        String hostId = (String) session.getAttributes().get("playerId");
        String hostName = (String) session.getAttributes().get("playerName");
        lobbyService.createLobby(lobbyCode, hostId, hostName, session);
        session.sendMessage(new TextMessage(
                "{\"type\":\"LOBBY_CREATED\",\"data\":{\"lobbyCode\":\"%s\",\"hostId\":\"%s\"}}"
                        .formatted(lobbyCode, hostId)));
    }

    private void handleJoinLobby(WebSocketSession session, String payload) throws Exception {
        String lobbyCode = payload.replaceAll(".*\"lobbyCode\":\"([^\"]+)\".*", "$1");
        if (!lobbyService.lobbyExists(lobbyCode)) {
            sendError(session, "LOBBY_NOT_FOUND", "Lobby nicht gefunden");
            return;
        }
        if (lobbyService.gameInProgress(lobbyCode)) {
            sendError(session, "GAME_IN_PROGRESS", "Das Spiel hat bereits begonnen");
            return;
        }
        if (lobbyService.lobbyFull(lobbyCode)) {
            sendError(session, "LOBBY_FULL", "Die Lobby ist voll (max. 4 Spieler)");
            return;
        }
        String playerId = (String) session.getAttributes().get("playerId");
        String playerName = (String) session.getAttributes().get("playerName");
        lobbyService.joinLobby(lobbyCode, playerId, playerName, session);
        broadcast(lobbyCode);
    }

    private void handleLeaveLobby(WebSocketSession session) throws Exception {
        String lobbyCode = lobbyService.getLobbyCodeForSession(session);
        if (lobbyCode != null) {
            lobbyService.removePlayer(lobbyCode, session);
            broadcast(lobbyCode);
        }
    }

    private void handlePlayerReady(WebSocketSession session, String payload) throws Exception {
        String lobbyCode = lobbyService.getLobbyCodeForSession(session);
        lobbyService.setReady(lobbyCode, session, payload.contains("\"ready\":true"));
        broadcast(lobbyCode);
    }

    private void handleSetGameMode(WebSocketSession session, String payload) throws Exception {
        String lobbyCode = lobbyService.getLobbyCodeForSession(session);
        String gameMode = payload.replaceAll(".*\"gameMode\":\"([^\"]+)\".*", "$1");
        lobbyService.setGameMode(lobbyCode, gameMode);
        broadcast(lobbyCode);
    }

    private void handleStartGame(WebSocketSession session) throws Exception {
        String lobbyCode = lobbyService.getLobbyCodeForSession(session);
        if (!lobbyService.isHost(lobbyCode, session)) {
            sendError(session, "NOT_HOST", "Nur der Host darf das Spiel starten");
            return;
        }
        lobbyService.startGame(lobbyCode);
        List<WebSocketSession> sessions = lobbyService.getSessions(lobbyCode);
        for (WebSocketSession s : sessions) {
            s.sendMessage(new TextMessage("{\"type\":\"GAME_STARTING\",\"data\":{}}"));
        }
        gameService.startGame(lobbyCode, lobbyService.getPlayers(lobbyCode), sessions);
    }

    private void handlePlayerInput(WebSocketSession session, String payload) {
        String lobbyCode = lobbyService.getLobbyCodeForSession(session);
        String playerId = (String) session.getAttributes().get("playerId");
        gameService.handleInput(lobbyCode, playerId,
                payload.contains("\"up\":true"),
                payload.contains("\"down\":true"),
                payload.contains("\"left\":true"),
                payload.contains("\"right\":true"),
                payload.contains("\"shoot\":true"));
    }

    private void handlePing(WebSocketSession session, String payload) throws Exception {
        long timestamp = Long.parseLong(payload.replaceAll(".*\"timestamp\":(\\d+).*", "$1"));
        session.sendMessage(new TextMessage(
                "{\"type\":\"PONG\",\"data\":{\"timestamp\":%d}}".formatted(timestamp)));
    }

    private void sendError(WebSocketSession session, String code, String message) throws Exception {
        session.sendMessage(new TextMessage(
                "{\"type\":\"ERROR\",\"data\":{\"code\":\"%s\",\"message\":\"%s\"}}".formatted(code, message)));
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
