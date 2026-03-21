package com.jets.backend;

public class Projectile {

    private final String id;
    private double x;
    private double y;
    private final double vx;
    private final double vy;
    private final String owner;

    private static final double SPEED = 10.0;

    public Projectile(String id, double spawnX, double spawnY, String owner) {
        this.id = id;
        this.x = spawnX;
        this.y = spawnY;
        this.vx = 0;
        this.vy = -SPEED;
        this.owner = owner;
    }

    public void update() {
        x += vx;
        y += vy;
    }

    public boolean isOutOfBounds() {
        return y < 0 || y > 1080 || x < 0 || x > 1920;
    }

    public String getId()    { return id; }
    public double getX()     { return x; }
    public double getY()     { return y; }
    public double getVx()    { return vx; }
    public double getVy()    { return vy; }
    public String getOwner() { return owner; }
}
