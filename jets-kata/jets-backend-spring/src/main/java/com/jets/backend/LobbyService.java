package com.jets.backend;

import org.springframework.stereotype.Service;

import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class LobbyService {

    private final Map<String, String> hostIds = new ConcurrentHashMap<>();             // lobbyCode -> hostId
    private final Map<String, List<WebSocketSession>> sessions = new ConcurrentHashMap<>(); // lobbyCode -> sessions

    public void createLobby(String lobbyCode, String hostId, WebSocketSession hostSession) {
        hostIds.put(lobbyCode, hostId);
        sessions.put(lobbyCode, new CopyOnWriteArrayList<>(List.of(hostSession)));
    }

    public void joinLobby(String lobbyCode, WebSocketSession session) {
        sessions.getOrDefault(lobbyCode, List.of()).add(session);
    }

    public boolean lobbyExists(String lobbyCode) {
        return hostIds.containsKey(lobbyCode);
    }

    public String getHostId(String lobbyCode) {
        return hostIds.get(lobbyCode);
    }

    public List<WebSocketSession> getSessions(String lobbyCode) {
        return sessions.getOrDefault(lobbyCode, List.of());
    }

    public String getLobbyCodeForSession(WebSocketSession session) {
        return sessions.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(s -> s.getId().equals(session.getId())))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
