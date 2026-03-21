package com.jets.backend.core.service;

import com.jets.backend.core.model.Lobby;
import com.jets.backend.core.model.LobbyPlayer;
import com.jets.backend.core.model.PlayerInfo;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class LobbyService {

    private static final String DEFAULT_GAME_MODE = "COOP";
    private static final List<String> PLAYER_COLORS = List.of(
            "#FF4444", "#4444FF", "#44FF44", "#FFFF44"
    );

    private final Map<String, Lobby> lobbies = new ConcurrentHashMap<>();
    private final Map<String, List<WebSocketSession>> lobbySessions = new ConcurrentHashMap<>();

    public Lobby createLobby(PlayerInfo creator) {
        String lobbyCode = generateLobbyCode();
        String color = PLAYER_COLORS.get(0);

        LobbyPlayer lobbyPlayer = new LobbyPlayer(creator.id(), creator.name(), false, color);
        Lobby lobby = new Lobby(lobbyCode, creator.id(), DEFAULT_GAME_MODE, new ArrayList<>(List.of(lobbyPlayer)));

        lobbies.put(lobbyCode, lobby);
        lobbySessions.put(lobbyCode, new CopyOnWriteArrayList<>(List.of(creator.session())));

        return lobby;
    }

    public void joinLobby(String lobbyCode, PlayerInfo player) {
        Lobby lobby = lobbies.get(lobbyCode);
        if (lobby == null) {
            throw new LobbyException(ErrorCode.LOBBY_NOT_FOUND, "Lobby " + lobbyCode + " existiert nicht");
        }
        if (lobby.started()) {
            throw new LobbyException(ErrorCode.GAME_IN_PROGRESS, "Das Spiel läuft bereits");
        }
        if (lobby.players().size() >= PLAYER_COLORS.size()) {
            throw new LobbyException(ErrorCode.LOBBY_FULL, "Die Lobby ist voll (max. " + PLAYER_COLORS.size() + " Spieler)");
        }
        String color = PLAYER_COLORS.get(lobby.players().size());

        lobby.players().add(new LobbyPlayer(player.id(), player.name(), false, color));
        lobbySessions.get(lobbyCode).add(player.session());
    }

    public void setPlayerReady(String lobbyCode, String playerId, boolean ready) {
        Lobby lobby = lobbies.get(lobbyCode);
        lobby.players().replaceAll(p ->
            p.id().equals(playerId) ? new LobbyPlayer(p.id(), p.name(), ready, p.color()) : p
        );
    }

    public Lobby getLobby(String lobbyCode) {
        return lobbies.get(lobbyCode);
    }

    public List<WebSocketSession> getLobbySessionList(String lobbyCode) {
        return lobbySessions.get(lobbyCode);
    }

    public void startGame(String lobbyCode, String hostId) {
        Lobby lobby = lobbies.get(lobbyCode);
        if (lobby == null) {
            throw new LobbyException(ErrorCode.LOBBY_NOT_FOUND, "Lobby existiert nicht");
        }
        if (lobby.players().size() < 2) {
            throw new LobbyException(ErrorCode.NOT_ENOUGH_PLAYERS, "Mindestens 2 Spieler werden benötigt");
        }
        if (!hostId.equals(lobby.hostId())) {
            throw new LobbyException(ErrorCode.NOT_HOST, "Nur der Host darf das Spiel starten");
        }
        boolean allReady = lobby.players().stream().allMatch(p -> p.ready());
        if (!allReady) {
            throw new LobbyException(ErrorCode.NOT_ALL_READY, "Nicht alle Spieler sind bereit");
        }
        lobby.start();
    }

    private String generateLobbyCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return code.toString();
    }
}

