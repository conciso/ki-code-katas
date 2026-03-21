package com.jets.backend.game;

import lombok.Data;

@Data
public class PlayerState {
    private double x;
    private double y;
    private int hp = 3;
    private boolean alive = true;
    private boolean invincible = false;
    private int score;
    private int lastProcessedInput;
    private PlayerInput currentInput;
    private int respawnTicksRemaining = 0;
    private int invincibilityTicksRemaining = 0;
    private int fireCooldownTicks = 0;
    private boolean shielded = false;
    private int rapidFireTicksRemaining = 0;
    private int speedBoostTicksRemaining = 0;

    public PlayerState(double x, double y) {
        this.x = x;
        this.y = y;
    }
}
