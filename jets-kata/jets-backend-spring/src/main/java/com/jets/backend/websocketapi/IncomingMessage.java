package com.jets.backend.websocketapi;

import tools.jackson.databind.JsonNode;

record IncomingMessage(MessageType type, JsonNode data) {}
