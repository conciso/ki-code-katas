package com.jets.backend.websocketapi;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.UUID;

@Component
public class JetsWebSocketHandler extends TextWebSocketHandler {

    private static final int SERVER_TICK_RATE = 30;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String playerId = "p" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        ObjectNode data = objectMapper.createObjectNode()
                .put("playerId", playerId)
                .put("serverTickRate", SERVER_TICK_RATE);

        ObjectNode msg = objectMapper.createObjectNode()
                .put("type", "CONNECTED");
        msg.set("data", data);

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    }
}
