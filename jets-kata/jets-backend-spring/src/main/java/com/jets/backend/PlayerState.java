package com.jets.backend;

public class PlayerState {

    private final String id;
    private final String name;
    private double x;
    private double y;
    private final int hp = 3;
    private int score = 0;

    private boolean up, down, left, right, shoot;
    private long lastShootTime = 0;

    private static final double SPEED = 5.0;
    private static final long SHOOT_COOLDOWN_MS = 200;

    public PlayerState(String id, String name, double spawnX, double spawnY) {
        this.id = id;
        this.name = name;
        this.x = spawnX;
        this.y = spawnY;
    }

    public void applyInput(boolean up, boolean down, boolean left, boolean right, boolean shoot) {
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
        this.shoot = shoot;
    }

    public void update() {
        if (right) x += SPEED;
        if (left)  x -= SPEED;
        if (down)  y += SPEED;
        if (up)    y -= SPEED;
    }

    public boolean canShoot() {
        return shoot && System.currentTimeMillis() - lastShootTime >= SHOOT_COOLDOWN_MS;
    }

    public void onShoot() {
        lastShootTime = System.currentTimeMillis();
    }

    public String getId()   { return id; }
    public String getName() { return name; }
    public double getX()    { return x; }
    public double getY()    { return y; }
    public int getHp()      { return hp; }
    public int getScore()   { return score; }
}
