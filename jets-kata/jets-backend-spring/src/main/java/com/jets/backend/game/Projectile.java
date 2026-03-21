package com.jets.backend.game;

import com.jets.backend.model.Client;
import lombok.Data;

@Data
public class Projectile {
    private String id;
    private double x;
    private double y;
    private double vx;
    private double vy;
    private Client owner;

    public Projectile(double x, double y, double vx, double vy, Client owner) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.owner = owner;
    }

    public String getOwnerId() {
        return owner != null ? owner.getId() : null;
    }
}
