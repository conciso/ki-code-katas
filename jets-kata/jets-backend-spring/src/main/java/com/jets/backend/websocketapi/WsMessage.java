package com.jets.backend.websocketapi;

import tools.jackson.databind.JsonNode;

record WsMessage<T>(MessageType type, T data) {

    static WsMessage<ConnectedData> connected(ConnectedData data) {
        return new WsMessage<>(MessageType.CONNECTED, data);
    }

    static WsMessage<LobbyCreatedData> lobbyCreated(LobbyCreatedData data) {
        return new WsMessage<>(MessageType.LOBBY_CREATED, data);
    }

    static WsMessage<LobbyStateData> lobbyState(LobbyStateData data) {
        return new WsMessage<>(MessageType.LOBBY_STATE, data);
    }

    static WsMessage<ErrorData> error(ErrorData data) {
        return new WsMessage<>(MessageType.ERROR, data);
    }

    static WsMessage<JsonNode> pong(JsonNode data) {
        return new WsMessage<>(MessageType.PONG, data);
    }

    static WsMessage<GameStartingData> gameStarting(GameStartingData data) {
        return new WsMessage<>(MessageType.GAME_STARTING, data);
    }
}
