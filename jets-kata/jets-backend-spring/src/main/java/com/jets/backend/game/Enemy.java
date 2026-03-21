package com.jets.backend.game;

import lombok.Data;

@Data
public class Enemy {
    private String id;
    private String type;
    private double x;
    private double y;
    private int hp;

    public Enemy(String type, double x, double y, int hp) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.hp = hp;
    }
}
