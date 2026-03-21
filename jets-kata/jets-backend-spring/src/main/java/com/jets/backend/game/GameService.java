package com.jets.backend.game;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import com.jets.backend.model.Client;

public class GameService {

    public static final int FFA_SCORE_LIMIT = 5000;
    public static final int FFA_TICK_LIMIT = 9000; // 5 Minuten bei 30 Ticks/s

    private static final double SPEED = 5.0;
    private static final double PROJECTILE_SPEED = SPEED * 2;
    private static final double FIELD_WIDTH = 1920;
    private static final double FIELD_HEIGHT = 1080;
    private static final int RESPAWN_TICKS = 90;
    private static final int INVINCIBILITY_TICKS = 60;
    private static final int FIRE_COOLDOWN_TICKS = 6;
    private static final int RAPID_FIRE_TICKS = 240; // 8 Sekunden
    private static final int SPEED_BOOST_TICKS = 180; // 6 Sekunden
    private static final double POWERUP_SPAWN_CHANCE = 0.20;

    private final Random random = new Random();
    private final AtomicLong entityCounter = new AtomicLong();

    public GameState startGame(Client... players) {
        GameState state = new GameState();
        for (int i = 0; i < players.length; i++) {
            double x = (FIELD_WIDTH / (players.length + 1)) * (i + 1);
            state.getPlayers().put(players[i], new PlayerState(x, FIELD_HEIGHT / 2));
        }
        return state;
    }

    public void applyInput(GameState state, Client player, PlayerInput input) {
        PlayerState ps = state.getPlayer(player);
        if (ps != null) {
            ps.setCurrentInput(input);
            ps.setLastProcessedInput(input.getSeq());
        }
    }

    public void hitPlayer(GameState state, Client player) {
        PlayerState ps = state.getPlayer(player);
        if (ps == null || !ps.isAlive() || ps.isInvincible()) return;
        if (ps.isShielded()) { ps.setShielded(false); return; }
        ps.setHp(ps.getHp() - 1);
        if (ps.getHp() <= 0) {
            ps.setAlive(false);
            ps.setRespawnTicksRemaining(RESPAWN_TICKS);
        }
    }

    public void hitPlayerByPlayer(GameState state, Client target, Client shooter) {
        PlayerState targetPs = state.getPlayer(target);
        PlayerState shooterPs = state.getPlayer(shooter);
        if (targetPs == null || !targetPs.isAlive() || targetPs.isInvincible()) return;
        if (targetPs.isShielded()) { targetPs.setShielded(false); return; }
        if (shooterPs != null) shooterPs.setScore(shooterPs.getScore() + 100);
        targetPs.setHp(targetPs.getHp() - 1);
        if (targetPs.getHp() <= 0) {
            targetPs.setAlive(false);
            targetPs.setRespawnTicksRemaining(RESPAWN_TICKS);
            targetPs.setScore(targetPs.getScore() - 200);
            if (shooterPs != null) shooterPs.setScore(shooterPs.getScore() + 500);
        }
    }

    public void hitEnemy(GameState state, Enemy enemy, Client shooter) {
        enemy.setHp(enemy.getHp() - 1);
        if (enemy.getHp() <= 0) {
            state.getEnemies().remove(enemy);
            PlayerState ps = state.getPlayer(shooter);
            if (ps != null) ps.setScore(ps.getScore() + scoreForType(enemy.getType()));
            if (random.nextDouble() < POWERUP_SPAWN_CHANCE) {
                PowerUp pu = new PowerUp(randomPowerUpType(), enemy.getX(), enemy.getY());
                pu.setId("pu" + entityCounter.incrementAndGet());
                state.getPowerUps().add(pu);
            }
        }
    }

    public void applyPowerUp(GameState state, Client player, PowerUpType type) {
        PlayerState ps = state.getPlayer(player);
        if (ps == null) return;
        switch (type) {
            case SHIELD -> ps.setShielded(true);
            case HEALTH_PACK -> ps.setHp(Math.min(3, ps.getHp() + 1));
            case RAPID_FIRE -> ps.setRapidFireTicksRemaining(RAPID_FIRE_TICKS);
            case SPEED_BOOST -> ps.setSpeedBoostTicksRemaining(SPEED_BOOST_TICKS);
        }
    }

    public boolean isFfaGameOver(GameState state) {
        if (state.getTick() >= FFA_TICK_LIMIT) return true;
        return state.getPlayers().values().stream()
            .anyMatch(ps -> ps.getScore() >= FFA_SCORE_LIMIT);
    }

    public void spawnWave(GameState state, int wave) {
        state.getEnemies().clear();
        if (wave <= 3) {
            spawnScouts(state, 3 + wave);
        } else if (wave <= 6) {
            spawnScouts(state, 3);
            spawnFighters(state, 2);
        } else if (wave <= 9) {
            spawnScouts(state, 3);
            spawnFighters(state, 2);
            spawnBombers(state, 1);
        } else if (wave == 10) {
            Enemy boss = new Enemy("BOSS", FIELD_WIDTH / 2, 0, 20);
            boss.setId("e" + entityCounter.incrementAndGet());
            state.getEnemies().add(boss);
            spawnScouts(state, 3);
            spawnFighters(state, 2);
        }
    }

    public void tick(GameState state) {
        state.setTick(state.getTick() + 1);

        for (var entry : state.getPlayers().entrySet()) {
            Client client = entry.getKey();
            PlayerState ps = entry.getValue();

            if (!ps.isAlive()) {
                ps.setRespawnTicksRemaining(ps.getRespawnTicksRemaining() - 1);
                if (ps.getRespawnTicksRemaining() <= 0) {
                    ps.setAlive(true);
                    ps.setHp(3);
                    ps.setInvincible(true);
                    ps.setInvincibilityTicksRemaining(INVINCIBILITY_TICKS);
                }
                continue;
            }

            if (ps.isInvincible()) {
                ps.setInvincibilityTicksRemaining(ps.getInvincibilityTicksRemaining() - 1);
                if (ps.getInvincibilityTicksRemaining() <= 0) ps.setInvincible(false);
            }
            if (ps.getRapidFireTicksRemaining() > 0) ps.setRapidFireTicksRemaining(ps.getRapidFireTicksRemaining() - 1);
            if (ps.getSpeedBoostTicksRemaining() > 0) ps.setSpeedBoostTicksRemaining(ps.getSpeedBoostTicksRemaining() - 1);
            if (ps.getFireCooldownTicks() > 0) ps.setFireCooldownTicks(ps.getFireCooldownTicks() - 1);

            PlayerInput input = ps.getCurrentInput();
            if (input == null) continue;

            double speed = ps.getSpeedBoostTicksRemaining() > 0 ? SPEED * 1.5 : SPEED;
            double dx = 0, dy = 0;
            if (input.isUp()) dy -= speed;
            if (input.isDown()) dy += speed;
            if (input.isLeft()) dx -= speed;
            if (input.isRight()) dx += speed;
            if (dx != 0 && dy != 0) { dx *= 0.707; dy *= 0.707; }

            ps.setX(Math.max(0, Math.min(FIELD_WIDTH, ps.getX() + dx)));
            ps.setY(Math.max(0, Math.min(FIELD_HEIGHT, ps.getY() + dy)));

            int cooldown = ps.getRapidFireTicksRemaining() > 0 ? FIRE_COOLDOWN_TICKS / 2 : FIRE_COOLDOWN_TICKS;
            if (input.isShoot() && ps.getFireCooldownTicks() == 0) {
                Projectile proj = new Projectile(ps.getX(), ps.getY(), 0, -PROJECTILE_SPEED, client);
                proj.setId("b" + entityCounter.incrementAndGet());
                state.getProjectiles().add(proj);
                ps.setFireCooldownTicks(cooldown);
            }
        }

        state.getProjectiles().forEach(p -> { p.setX(p.getX() + p.getVx()); p.setY(p.getY() + p.getVy()); });
        state.getProjectiles().removeIf(p ->
            p.getX() < 0 || p.getX() > FIELD_WIDTH || p.getY() < 0 || p.getY() > FIELD_HEIGHT);
    }

    private void spawnScouts(GameState state, int count) {
        for (int i = 0; i < count; i++) {
            Enemy e = new Enemy("SCOUT", 200 + i * 200.0, 0, 1);
            e.setId("e" + entityCounter.incrementAndGet());
            state.getEnemies().add(e);
        }
    }

    private void spawnFighters(GameState state, int count) {
        for (int i = 0; i < count; i++) {
            Enemy e = new Enemy("FIGHTER", 300 + i * 200.0, 0, 2);
            e.setId("e" + entityCounter.incrementAndGet());
            state.getEnemies().add(e);
        }
    }

    private void spawnBombers(GameState state, int count) {
        for (int i = 0; i < count; i++) {
            Enemy e = new Enemy("BOMBER", 400 + i * 200.0, 0, 4);
            e.setId("e" + entityCounter.incrementAndGet());
            state.getEnemies().add(e);
        }
    }

    private int scoreForType(String type) {
        return switch (type) {
            case "SCOUT" -> 100;
            case "FIGHTER" -> 250;
            case "BOMBER" -> 500;
            case "BOSS" -> 2000;
            default -> 0;
        };
    }

    private PowerUpType randomPowerUpType() {
        PowerUpType[] types = PowerUpType.values();
        return types[random.nextInt(types.length)];
    }
}
