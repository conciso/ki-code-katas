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
        String message = """
                {"type":"CONNECTED","data":{"playerId":"%s","serverTickRate":30}}
                """.formatted(playerId).strip();
        session.sendMessage(new TextMessage(message));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (message.getPayload().contains("\"type\":\"CREATE_LOBBY\"")) {
            String lobbyCode = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
            String hostId = (String) session.getAttributes().get("playerId");
            lobbyService.createLobby(lobbyCode, hostId, session);
            String response = """
                    {"type":"LOBBY_CREATED","data":{"lobbyCode":"%s","hostId":"%s"}}
                    """.formatted(lobbyCode, hostId).strip();
            session.sendMessage(new TextMessage(response));
        } else if (message.getPayload().contains("\"type\":\"JOIN_LOBBY\"")) {
            String lobbyCode = message.getPayload().replaceAll(".*\"lobbyCode\":\"([^\"]+)\".*", "$1");
            String hostId = lobbyService.getHostId(lobbyCode);
            lobbyService.joinLobby(lobbyCode, session);
            String response = """
                    {"type":"LOBBY_STATE","data":{"lobbyCode":"%s","hostId":"%s"}}
                    """.formatted(lobbyCode, hostId).strip();
            for (WebSocketSession s : lobbyService.getSessions(lobbyCode)) {
                s.sendMessage(new TextMessage(response));
            }
        } else if (message.getPayload().contains("\"type\":\"START_GAME\"")) {
            String lobbyCode = lobbyService.getLobbyCodeForSession(session);
            String response = "{\"type\":\"GAME_STARTING\",\"data\":{}}";
            for (WebSocketSession s : lobbyService.getSessions(lobbyCode)) {
                s.sendMessage(new TextMessage(response));
            }
        } else if (message.getPayload().contains("\"type\":\"PING\"")) {
            String payload = message.getPayload();
            long timestamp = Long.parseLong(payload.replaceAll(".*\"timestamp\":(\\d+).*", "$1"));
            String response = "{\"type\":\"PONG\",\"data\":{\"timestamp\":%d}}".formatted(timestamp);
            session.sendMessage(new TextMessage(response));
        }
    }
}
