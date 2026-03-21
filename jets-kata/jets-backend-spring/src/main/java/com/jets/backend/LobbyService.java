package com.jets.backend;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class LobbyService {

    private final Map<String, WebSocketSession> sessionRegistry = new ConcurrentHashMap<>(); // sessionId -> session
    private final Map<String, String> hostPlayerIds = new ConcurrentHashMap<>();              // lobbyCode -> playerId (für Protokoll)
    private final Map<String, String> hostSessionIds = new ConcurrentHashMap<>();             // lobbyCode -> sessionId (für Validierung)
    private final Map<String, List<String>> lobbySessionIds = new ConcurrentHashMap<>();      // lobbyCode -> sessionIds
    private final Map<String, List<Player>> lobbyPlayers = new ConcurrentHashMap<>();         // lobbyCode -> players
    private final Map<String, String> gameModes = new ConcurrentHashMap<>();                  // lobbyCode -> gameMode
    private final java.util.Set<String> activeGames = ConcurrentHashMap.newKeySet();          // lobbyCode

    public void registerSession(WebSocketSession session) {
        sessionRegistry.put(session.getId(), session);
    }

    public void createLobby(String lobbyCode, String hostPlayerId, String hostName, WebSocketSession hostSession) {
        hostPlayerIds.put(lobbyCode, hostPlayerId);
        hostSessionIds.put(lobbyCode, hostSession.getId());
        lobbySessionIds.put(lobbyCode, new CopyOnWriteArrayList<>(List.of(hostSession.getId())));
        lobbyPlayers.put(lobbyCode, new CopyOnWriteArrayList<>(List.of(new Player(hostPlayerId, hostName))));
        gameModes.put(lobbyCode, "COOP");
    }

    public void joinLobby(String lobbyCode, String playerId, String playerName, WebSocketSession session) {
        lobbySessionIds.getOrDefault(lobbyCode, List.of()).add(session.getId());
        lobbyPlayers.getOrDefault(lobbyCode, List.of()).add(new Player(playerId, playerName));
    }

    public void removePlayer(String lobbyCode, WebSocketSession session) {
        String sessionId = session.getId();
        lobbySessionIds.getOrDefault(lobbyCode, List.of()).removeIf(id -> id.equals(sessionId));
        String playerId = (String) session.getAttributes().get("playerId");
        lobbyPlayers.getOrDefault(lobbyCode, List.of()).removeIf(p -> p.getId().equals(playerId));

        // Host-Wechsel wenn Host die Lobby verlässt
        if (sessionId.equals(hostSessionIds.get(lobbyCode))) {
            List<String> remaining = lobbySessionIds.get(lobbyCode);
            if (!remaining.isEmpty()) {
                String newHostSessionId = remaining.get(0);
                hostSessionIds.put(lobbyCode, newHostSessionId);
                WebSocketSession newHostSession = sessionRegistry.get(newHostSessionId);
                if (newHostSession != null) {
                    hostPlayerIds.put(lobbyCode, (String) newHostSession.getAttributes().get("playerId"));
                }
            }
        }
    }

    public void setReady(String lobbyCode, WebSocketSession session, boolean ready) {
        String playerId = (String) session.getAttributes().get("playerId");
        lobbyPlayers.getOrDefault(lobbyCode, List.of()).stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .ifPresent(p -> p.setReady(ready));
    }

    public void setGameMode(String lobbyCode, String gameMode) {
        gameModes.put(lobbyCode, gameMode);
    }

    public boolean lobbyExists(String lobbyCode) {
        return hostPlayerIds.containsKey(lobbyCode);
    }

    public boolean lobbyFull(String lobbyCode) {
        return lobbySessionIds.getOrDefault(lobbyCode, List.of()).size() >= 4;
    }

    public void startGame(String lobbyCode) {
        activeGames.add(lobbyCode);
    }

    public boolean gameInProgress(String lobbyCode) {
        return activeGames.contains(lobbyCode);
    }

    public boolean isHost(String lobbyCode, WebSocketSession session) {
        return session.getId().equals(hostSessionIds.get(lobbyCode));
    }

    public String getHostId(String lobbyCode) {
        return hostPlayerIds.get(lobbyCode);
    }

    public List<WebSocketSession> getSessions(String lobbyCode) {
        return lobbySessionIds.getOrDefault(lobbyCode, List.of()).stream()
                .map(sessionRegistry::get)
                .filter(s -> s != null && s.isOpen())
                .toList();
    }

    public String getLobbyCodeForSession(WebSocketSession session) {
        return lobbySessionIds.entrySet().stream()
                .filter(e -> e.getValue().contains(session.getId()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public String buildLobbyState(String lobbyCode) {
        String hostId = hostPlayerIds.get(lobbyCode);
        String gameMode = gameModes.getOrDefault(lobbyCode, "COOP");
        List<Player> players = lobbyPlayers.getOrDefault(lobbyCode, List.of());

        StringBuilder playersJson = new StringBuilder("[");
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            playersJson.append("{\"id\":\"%s\",\"name\":\"%s\",\"ready\":%s}"
                    .formatted(p.getId(), p.getName(), p.isReady()));
            if (i < players.size() - 1) playersJson.append(",");
        }
        playersJson.append("]");

        return "{\"type\":\"LOBBY_STATE\",\"data\":{\"lobbyCode\":\"%s\",\"hostId\":\"%s\",\"gameMode\":\"%s\",\"players\":%s}}"
                .formatted(lobbyCode, hostId, gameMode, playersJson);
    }
}
