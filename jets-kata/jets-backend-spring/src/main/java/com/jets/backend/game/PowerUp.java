package com.jets.backend.game;

import lombok.Data;

@Data
public class PowerUp {
    private String id;
    private PowerUpType type;
    private double x;
    private double y;

    public PowerUp(PowerUpType type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }
}
