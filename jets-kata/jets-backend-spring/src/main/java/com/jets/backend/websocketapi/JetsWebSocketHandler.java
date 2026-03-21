package com.jets.backend.websocketapi;

import com.jets.backend.core.model.Lobby;
import com.jets.backend.core.model.PlayerInfo;
import com.jets.backend.core.service.LobbyException;
import com.jets.backend.core.service.LobbyService;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JetsWebSocketHandler extends TextWebSocketHandler {

    private static final int SERVER_TICK_RATE = 30;

    private final ObjectMapper objectMapper;
    private final LobbyService lobbyService;
    private final Map<String, PlayerInfo> players = new ConcurrentHashMap<>();
    private final Map<String, String> playerLobby = new ConcurrentHashMap<>();
    private final Map<MessageType, MessageHandler> handlers = new HashMap<>();

    public JetsWebSocketHandler(ObjectMapper objectMapper, LobbyService lobbyService) {
        this.objectMapper = objectMapper;
        this.lobbyService = lobbyService;
        initializeHandlers();
    }

    private void initializeHandlers() {
        handlers.put(MessageType.CREATE_LOBBY, (session, data) -> handleCreateLobby(session));
        handlers.put(MessageType.JOIN_LOBBY, (session, data) -> handleJoinLobby(session, data.get("lobbyCode").stringValue()));
        handlers.put(MessageType.PLAYER_READY, (session, data) -> handlePlayerReady(session, data.get("ready").booleanValue()));
        handlers.put(MessageType.PING, this::handlePing);
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
        MessageHandler handler = handlers.get(msg.type());

        if (handler != null) {
            handler.handle(session, msg.data());
        }
    }

    private void handleCreateLobby(WebSocketSession session) throws Exception {
        PlayerInfo player = players.get(session.getId());
        Lobby lobby = lobbyService.createLobby(player);

        playerLobby.put(session.getId(), lobby.code());
        send(session, WsMessage.lobbyCreated(new LobbyCreatedData(lobby.code(), lobby.hostId())));
        broadcastLobbyState(lobby.code());
    }

    private void handleJoinLobby(WebSocketSession session, String lobbyCode) throws Exception {
        PlayerInfo player = players.get(session.getId());
        try {
            lobbyService.joinLobby(lobbyCode, player);
        } catch (LobbyException e) {
            send(session, WsMessage.error(new ErrorData(e.getErrorCode(), e.getMessage())));
            return;
        }
        playerLobby.put(session.getId(), lobbyCode);
        broadcastLobbyState(lobbyCode);
    }

    private void handlePlayerReady(WebSocketSession session, boolean ready) throws Exception {
        PlayerInfo player = players.get(session.getId());
        String lobbyCode = playerLobby.get(session.getId());
        lobbyService.setPlayerReady(lobbyCode, player.id(), ready);
        broadcastLobbyState(lobbyCode);
    }

    private void broadcastLobbyState(String lobbyCode) throws Exception {
        Lobby lobby = lobbyService.getLobby(lobbyCode);
        WsMessage<LobbyStateData> msg = WsMessage.lobbyState(toLobbyStateData(lobby));
        for (WebSocketSession s : lobbyService.getLobbySessionList(lobbyCode)) {
            if (s.isOpen()) send(s, msg);
        }
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

    private void handlePing(WebSocketSession session, tools.jackson.databind.JsonNode data) throws Exception {
        send(session, WsMessage.pong(data));
    }
}
