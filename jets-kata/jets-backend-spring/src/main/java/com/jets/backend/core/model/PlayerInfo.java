package com.jets.backend.core.model;

import org.springframework.web.socket.WebSocketSession;

public record PlayerInfo(String id, String name, WebSocketSession session) {}

