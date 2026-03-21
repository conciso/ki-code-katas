package com.jets.backend;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    private final Map<String, GameInstance> games = new ConcurrentHashMap<>();

    public void startGame(String lobbyCode, List<Player> players, List<WebSocketSession> sessions) {
        GameInstance game = new GameInstance(lobbyCode, players, sessions);
        games.put(lobbyCode, game);
        game.start();
    }

    public void handleInput(String lobbyCode, String playerId,
                            boolean up, boolean down, boolean left, boolean right, boolean shoot) {
        GameInstance game = games.get(lobbyCode);
        if (game != null) {
            game.applyInput(playerId, up, down, left, right, shoot);
        }
    }
}
