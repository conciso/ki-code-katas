package com.jets.backend.websocketapi;

import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JetsWebSocketHandler extends TextWebSocketHandler {

    private static final int SERVER_TICK_RATE = 30;
    private static final String DEFAULT_GAME_MODE = "COOP";
    private static final List<String> PLAYER_COLORS = List.of(
            "#FF4444", "#4444FF", "#44FF44", "#FFFF44"
    );

    private final ObjectMapper objectMapper;
    private final Map<String, PlayerInfo> players = new ConcurrentHashMap<>();
    private final Map<String, Lobby> lobbies = new ConcurrentHashMap<>();

    public JetsWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String playerName = extractQueryParam(session, "playerName");
        String playerId = "p" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        players.put(session.getId(), new PlayerInfo(playerId, playerName, session));

        send(session, WsMessage.connected(new ConnectedData(playerId, SERVER_TICK_RATE)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        IncomingMessage msg = objectMapper.readValue(message.getPayload(), IncomingMessage.class);

        if ("CREATE_LOBBY".equals(msg.type())) {
            handleCreateLobby(session);
        }
    }

    private void handleCreateLobby(WebSocketSession session) throws Exception {
        PlayerInfo player = players.get(session.getId());
        String lobbyCode = generateLobbyCode();
        String color = PLAYER_COLORS.get(0);

        LobbyPlayer lobbyPlayer = new LobbyPlayer(player.id(), player.name(), false, color);
        Lobby lobby = new Lobby(lobbyCode, player.id(), DEFAULT_GAME_MODE, new ArrayList<>(List.of(lobbyPlayer)));
        lobbies.put(lobbyCode, lobby);

        send(session, WsMessage.lobbyCreated(new LobbyCreatedData(lobbyCode, player.id())));
        send(session, WsMessage.lobbyState(toLobbyStateData(lobby)));
    }

    private LobbyStateData toLobbyStateData(Lobby lobby) {
        List<LobbyPlayerData> playerData = lobby.players().stream()
                .map(p -> new LobbyPlayerData(p.id(), p.name(), p.ready(), p.color()))
                .toList();
        return new LobbyStateData(lobby.code(), lobby.hostId(), lobby.gameMode(), playerData);
    }

    private void send(WebSocketSession session, WsMessage<?> message) throws Exception {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
    }

    private String generateLobbyCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return code.toString();
    }

    private String extractQueryParam(WebSocketSession session, String param) {
        String query = session.getUri().getQuery();
        if (query == null) return "";
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(param)) return kv[1];
        }
        return "";
    }

    private record PlayerInfo(String id, String name, WebSocketSession session) {}
    private record LobbyPlayer(String id, String name, boolean ready, String color) {}
    private record Lobby(String code, String hostId, String gameMode, List<LobbyPlayer> players) {}
}
