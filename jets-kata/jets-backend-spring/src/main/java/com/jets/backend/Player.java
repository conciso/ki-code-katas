package com.jets.backend;

public class Player {

    private final String id;
    private final String name;
    private boolean ready;

    public Player(String id, String name) {
        this.id = id;
        this.name = name;
        this.ready = false;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
}
