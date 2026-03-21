package com.jets.backend.websocketapi;

import java.util.List;

record GameStartingData(
    int countdown,
    String gameMode,
    int fieldWidth,
    int fieldHeight,
    List<GameStartingPlayer> players
) {}

record GameStartingPlayer(
    String id,
    String name,
    String color,
    int spawnX,
    int spawnY
) {}

