package com.jets.backend.websocketapi;

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
}
