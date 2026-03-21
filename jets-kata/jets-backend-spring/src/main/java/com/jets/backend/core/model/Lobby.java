package com.jets.backend.core.model;

import java.util.List;

public class Lobby {
    private final String code;
    private final String hostId;
    private final String gameMode;
    private final List<LobbyPlayer> players;
    private boolean started;

    public Lobby(String code, String hostId, String gameMode, List<LobbyPlayer> players) {
        this.code = code;
        this.hostId = hostId;
        this.gameMode = gameMode;
        this.players = players;
        this.started = false;
    }

    public String code() { return code; }
    public String hostId() { return hostId; }
    public String gameMode() { return gameMode; }
    public List<LobbyPlayer> players() { return players; }
    public boolean started() { return started; }
    public void start() { this.started = true; }
}
