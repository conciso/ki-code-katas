package com.jets.backend;

import java.util.Arrays;

public enum MessageType {
    CREATE_LOBBY,
    JOIN_LOBBY,
    LEAVE_LOBBY,
    PLAYER_READY,
    SET_GAME_MODE,
    START_GAME,
    PLAYER_INPUT,
    PING,
    UNKNOWN;

    public static MessageType from(String payload) {
        return Arrays.stream(values())
                .filter(t -> t != UNKNOWN && payload.contains("\"type\":\"" + t.name() + "\""))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
