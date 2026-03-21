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
    private final Map<String, String> hostIds = new ConcurrentHashMap<>();                    // lobbyCode -> hostId
    private final Map<String, List<String>> lobbySessionIds = new ConcurrentHashMap<>();      // lobbyCode -> sessionIds
    private final java.util.Set<String> activeGames = ConcurrentHashMap.newKeySet();          // lobbyCode

    public void registerSession(WebSocketSession session) {
        sessionRegistry.put(session.getId(), session);
    }

    public void createLobby(String lobbyCode, String hostId, WebSocketSession hostSession) {
        hostIds.put(lobbyCode, hostId);
        lobbySessionIds.put(lobbyCode, new CopyOnWriteArrayList<>(List.of(hostSession.getId())));
    }

    public void joinLobby(String lobbyCode, WebSocketSession session) {
        lobbySessionIds.getOrDefault(lobbyCode, List.of()).add(session.getId());
    }

    public boolean lobbyExists(String lobbyCode) {
        return hostIds.containsKey(lobbyCode);
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
        String hostId = hostIds.get(lobbyCode);
        return hostId != null && hostId.equals(session.getAttributes().get("playerId"));
    }

    public String getHostId(String lobbyCode) {
        return hostIds.get(lobbyCode);
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
}
