package com.jets.backend.core.service;

import com.jets.backend.core.model.PlayerInfo;
import org.springframework.stereotype.Service;

@Service
public class JoinLobbyUseCase {

    private final LobbyService lobbyService;

    public JoinLobbyUseCase(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    public void execute(String lobbyCode, PlayerInfo player) {
        lobbyService.joinLobby(lobbyCode, player);
    }
}

