package com.jets.backend.core.service;

import com.jets.backend.core.model.Lobby;
import org.springframework.stereotype.Service;

@Service
public class StartGameUseCase {

    private final LobbyService lobbyService;

    public StartGameUseCase(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    public void execute(String lobbyCode, String hostId) {
        Lobby lobby = lobbyService.getLobby(lobbyCode);
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
}

