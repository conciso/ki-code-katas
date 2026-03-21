package com.jets.backend.core.service;

import org.springframework.stereotype.Service;

@Service
public class SetPlayerReadyUseCase {

    private final LobbyService lobbyService;

    public SetPlayerReadyUseCase(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    public void execute(String lobbyCode, String playerId, boolean ready) {
        lobbyService.setPlayerReady(lobbyCode, playerId, ready);
    }
}

