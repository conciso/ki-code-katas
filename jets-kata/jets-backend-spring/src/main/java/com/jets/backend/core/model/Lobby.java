package com.jets.backend.core.model;

import java.util.List;

public record Lobby(String code, String hostId, String gameMode, List<LobbyPlayer> players) {}

