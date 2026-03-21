package com.jets.backend.websocketapi;

import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.JsonNode;

@FunctionalInterface
public interface MessageHandler {
    void handle(WebSocketSession session, JsonNode data) throws Exception;
}

