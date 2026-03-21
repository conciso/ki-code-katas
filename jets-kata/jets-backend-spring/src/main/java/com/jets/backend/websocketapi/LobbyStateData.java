package com.jets.backend.websocketapi;

import java.util.List;

record LobbyStateData(String lobbyCode, String hostId, String gameMode, List<LobbyPlayerData> players) {}
