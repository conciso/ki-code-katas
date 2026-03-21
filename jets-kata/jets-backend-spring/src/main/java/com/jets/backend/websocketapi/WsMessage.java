package com.jets.backend.websocketapi;

record WsMessage<T>(String type, T data) {

    static WsMessage<ConnectedData> connected(ConnectedData data) {
        return new WsMessage<>("CONNECTED", data);
    }

    static WsMessage<LobbyCreatedData> lobbyCreated(LobbyCreatedData data) {
        return new WsMessage<>("LOBBY_CREATED", data);
    }

    static WsMessage<LobbyStateData> lobbyState(LobbyStateData data) {
        return new WsMessage<>("LOBBY_STATE", data);
    }
}
