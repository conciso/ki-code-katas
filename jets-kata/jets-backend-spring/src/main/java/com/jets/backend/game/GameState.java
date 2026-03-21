package com.jets.backend.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.jets.backend.model.Client;
import lombok.Data;

@Data
public class GameState {
    private Map<Client, PlayerState> players = new HashMap<>();
    private List<Projectile> projectiles = new ArrayList<>();
    private List<Enemy> enemies = new ArrayList<>();
    private List<PowerUp> powerUps = new ArrayList<>();
    private int tick;
    private int wave;

    public PlayerState getPlayer(Client client) {
        return players.get(client);
    }
}
