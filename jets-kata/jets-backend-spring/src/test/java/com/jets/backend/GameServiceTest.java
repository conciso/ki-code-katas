package com.jets.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.jets.backend.game.Enemy;
import com.jets.backend.game.GameService;
import com.jets.backend.game.GameState;
import com.jets.backend.game.PlayerInput;
import com.jets.backend.game.PowerUpType;
import com.jets.backend.model.Client;

class GameServiceTest {

	@Nested
	class PlayerStateTests {

		@Test
		void playerStartsWithThreeHp() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			Client bob = new Client("Bob");

			GameState state = gameService.startGame(alice, bob);

			assertThat(state.getPlayer(alice).getHp()).isEqualTo(3);
		}

		@Test
		void playerStartsAlive() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			Client bob = new Client("Bob");

			GameState state = gameService.startGame(alice, bob);

			assertThat(state.getPlayer(alice).isAlive()).isTrue();
		}

		@Test
		void playerScoreStartsAtZero() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");

			GameState state = gameService.startGame(alice, new Client("Bob"));

			assertThat(state.getPlayer(alice).getScore()).isEqualTo(0);
		}

		@Test
		void multiplePlayersHaveUniqueStartPositions() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			Client bob = new Client("Bob");

			GameState state = gameService.startGame(alice, bob);

			assertThat(state.getPlayer(alice).getX()).isNotEqualTo(state.getPlayer(bob).getX());
		}

		@Test
		void playerInputSeqIsTracked() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");

			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.applyInput(state, alice, new PlayerInput(false, false, false, false, false, 42));

			assertThat(state.getPlayer(alice).getLastProcessedInput()).isEqualTo(42);
		}

		@Test
		void gameStartsWithTickZero() {
			GameService gameService = new GameService();

			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));

			assertThat(state.getTick()).isEqualTo(0);
		}

		@Test
		void playerStartsAtFieldHeightCenter() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");

			GameState state = gameService.startGame(alice, new Client("Bob"));

			assertThat(state.getPlayer(alice).getY()).isEqualTo(540.0); // FIELD_HEIGHT / 2
		}

		@Test
		void startGameWithThreePlayersHasThreeStates() {
			GameService gameService = new GameService();

			GameState state = gameService.startGame(
				new Client("Alice"), new Client("Bob"), new Client("Carol"));

			assertThat(state.getPlayers()).hasSize(3);
		}

		@Test
		void startGameWithFourPlayersHasFourStates() {
			GameService gameService = new GameService();

			GameState state = gameService.startGame(
				new Client("Alice"), new Client("Bob"), new Client("Carol"), new Client("Dave"));

			assertThat(state.getPlayers()).hasSize(4);
		}
	}

	@Nested
	class MovementTests {

		@Test
		void playerPositionChangesOnInput() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			double initialY = state.getPlayer(alice).getY();

			gameService.applyInput(state, alice, new PlayerInput(false, true, false, false, false, 1));
			gameService.tick(state);

			assertThat(state.getPlayer(alice).getY()).isGreaterThan(initialY);
		}

		@Test
		void movingUpDecreasesY() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			double initialY = state.getPlayer(alice).getY();

			gameService.applyInput(state, alice, new PlayerInput(true, false, false, false, false, 1));
			gameService.tick(state);

			assertThat(state.getPlayer(alice).getY()).isLessThan(initialY);
		}

		@Test
		void movingLeftDecreasesX() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			double initialX = state.getPlayer(alice).getX();

			gameService.applyInput(state, alice, new PlayerInput(false, false, true, false, false, 1));
			gameService.tick(state);

			assertThat(state.getPlayer(alice).getX()).isLessThan(initialX);
		}

		@Test
		void movingRightIncreasesX() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			double initialX = state.getPlayer(alice).getX();

			gameService.applyInput(state, alice, new PlayerInput(false, false, false, true, false, 1));
			gameService.tick(state);

			assertThat(state.getPlayer(alice).getX()).isGreaterThan(initialX);
		}

		@Test
		void noMovementWithoutInput() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			double initialX = state.getPlayer(alice).getX();
			double initialY = state.getPlayer(alice).getY();

			gameService.tick(state);

			assertThat(state.getPlayer(alice).getX()).isEqualTo(initialX);
			assertThat(state.getPlayer(alice).getY()).isEqualTo(initialY);
		}

		@Test
		void diagonalMovementIsNormalized() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			double startX = state.getPlayer(alice).getX();

			gameService.applyInput(state, alice, new PlayerInput(true, false, true, false, false, 1));
			gameService.tick(state);

			double dx = Math.abs(state.getPlayer(alice).getX() - startX);
			assertThat(dx).isLessThan(5.0);
		}

		@Test
		void playerCannotMoveOutsideFieldBounds() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));

			for (int i = 0; i < 1000; i++) {
				gameService.applyInput(state, alice, new PlayerInput(false, false, true, false, false, i));
				gameService.tick(state);
			}

			assertThat(state.getPlayer(alice).getX()).isGreaterThanOrEqualTo(0);
		}

		@Test
		void playerCannotMoveBelowField() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));

			for (int i = 0; i < 1000; i++) {
				gameService.applyInput(state, alice, new PlayerInput(false, true, false, false, false, i));
				gameService.tick(state);
			}

			assertThat(state.getPlayer(alice).getY()).isLessThanOrEqualTo(1080);
		}

		@Test
		void playerCannotMoveRightOfField() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));

			for (int i = 0; i < 1000; i++) {
				gameService.applyInput(state, alice, new PlayerInput(false, false, false, true, false, i));
				gameService.tick(state);
			}

			assertThat(state.getPlayer(alice).getX()).isLessThanOrEqualTo(1920);
		}

		@Test
		void playerCannotMoveAboveField() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));

			for (int i = 0; i < 1000; i++) {
				gameService.applyInput(state, alice, new PlayerInput(true, false, false, false, false, i));
				gameService.tick(state);
			}

			assertThat(state.getPlayer(alice).getY()).isGreaterThanOrEqualTo(0);
		}

		@Test
		void playerMovesExactFivePixelsRightPerTick() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			double startX = state.getPlayer(alice).getX();

			gameService.applyInput(state, alice, new PlayerInput(false, false, false, true, false, 1));
			gameService.tick(state);

			assertThat(state.getPlayer(alice).getX()).isEqualTo(startX + 5.0);
		}

		@Test
		void playerMovesExactFivePixelsDownPerTick() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			double startY = state.getPlayer(alice).getY();

			gameService.applyInput(state, alice, new PlayerInput(false, true, false, false, false, 1));
			gameService.tick(state);

			assertThat(state.getPlayer(alice).getY()).isEqualTo(startY + 5.0);
		}

		@Test
		void speedBoostExactMultiplierIsOnePointFive() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.applyPowerUp(state, alice, PowerUpType.SPEED_BOOST);
			double startY = state.getPlayer(alice).getY();

			gameService.applyInput(state, alice, new PlayerInput(false, true, false, false, false, 1));
			gameService.tick(state);

			assertThat(state.getPlayer(alice).getY()).isEqualTo(startY + 7.5); // 5 * 1.5
		}
	}

	@Nested
	class TickTests {

		@Test
		void tickIncrementsTickCounter() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));

			gameService.tick(state);
			gameService.tick(state);

			assertThat(state.getTick()).isEqualTo(2);
		}
	}

	@Nested
	class ShootingTests {

		@Test
		void shootingCreatesProjectile() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));

			gameService.applyInput(state, alice, new PlayerInput(false, false, false, false, true, 1));
			gameService.tick(state);

			assertThat(state.getProjectiles()).isNotEmpty();
		}

		@Test
		void projectileMovesEachTick() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.applyInput(state, alice, new PlayerInput(false, false, false, false, true, 1));
			gameService.tick(state);
			double initialY = state.getProjectiles().get(0).getY();

			gameService.tick(state);

			assertThat(state.getProjectiles().get(0).getY()).isNotEqualTo(initialY);
		}

		@Test
		void projectileRemovedWhenLeavingField() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.applyInput(state, alice, new PlayerInput(false, false, false, false, true, 1));
			gameService.tick(state);
			gameService.applyInput(state, alice, new PlayerInput(false, false, false, false, false, 2));

			for (int i = 0; i < 200; i++) gameService.tick(state);

			assertThat(state.getProjectiles()).isEmpty();
		}

		@Test
		void projectileSpeedIsTwicePlayerSpeed() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			double startY = state.getPlayer(alice).getY();
			gameService.applyInput(state, alice, new PlayerInput(false, false, false, false, true, 1));
			gameService.tick(state);
			double projectileY = state.getProjectiles().get(0).getY();
			double playerMovedUp = startY - projectileY;

			gameService.applyInput(state, alice, new PlayerInput(true, false, false, false, false, 2));
			double playerYBefore = state.getPlayer(alice).getY();
			gameService.tick(state);
			double playerMoved = playerYBefore - state.getPlayer(alice).getY();

			assertThat(playerMovedUp).isEqualTo(playerMoved * 2);
		}

		@Test
		void projectileHasCorrectOwner() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));

			gameService.applyInput(state, alice, new PlayerInput(false, false, false, false, true, 1));
			gameService.tick(state);

			assertThat(state.getProjectiles().get(0).getOwner()).isEqualTo(alice);
		}

		@Test
		void fireCooldownPreventsRapidFire() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));

			for (int i = 0; i < 10; i++) {
				gameService.applyInput(state, alice, new PlayerInput(false, false, false, false, true, i));
				gameService.tick(state);
			}

			assertThat(state.getProjectiles().size()).isLessThanOrEqualTo(5);
		}

		@Test
		void deadPlayerCannotShoot() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);

			gameService.applyInput(state, alice, new PlayerInput(false, false, false, false, true, 1));
			gameService.tick(state);

			assertThat(state.getProjectiles()).isEmpty();
		}

		@Test
		void twoProjectilesCanExistSimultaneously() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.applyInput(state, alice, new PlayerInput(false, false, false, false, true, 1));
			gameService.tick(state);
			gameService.applyInput(state, alice, new PlayerInput(false, false, false, false, false, 2));
			for (int i = 0; i < 6; i++) gameService.tick(state);
			gameService.applyInput(state, alice, new PlayerInput(false, false, false, false, true, 3));
			gameService.tick(state);

			assertThat(state.getProjectiles().size()).isEqualTo(2);
		}

		@Test
		void projectileInitialXEqualsPlayerX() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			double playerX = state.getPlayer(alice).getX();

			gameService.applyInput(state, alice, new PlayerInput(false, false, false, false, true, 1));
			gameService.tick(state);

			// vx=0, so X never changes from initial player X
			assertThat(state.getProjectiles().get(0).getX()).isEqualTo(playerX);
		}

		@Test
		void projectileVxIsZero() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));

			gameService.applyInput(state, alice, new PlayerInput(false, false, false, false, true, 1));
			gameService.tick(state);

			assertThat(state.getProjectiles().get(0).getVx()).isEqualTo(0.0);
		}

		@Test
		void multiplePlayersCanShootSimultaneously() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			Client bob = new Client("Bob");
			GameState state = gameService.startGame(alice, bob);

			gameService.applyInput(state, alice, new PlayerInput(false, false, false, false, true, 1));
			gameService.applyInput(state, bob, new PlayerInput(false, false, false, false, true, 1));
			gameService.tick(state);

			assertThat(state.getProjectiles().size()).isEqualTo(2);
		}

		@Test
		void fireCooldownIsSetToSixAfterShot() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));

			gameService.applyInput(state, alice, new PlayerInput(false, false, false, false, true, 1));
			gameService.tick(state);

			assertThat(state.getPlayer(alice).getFireCooldownTicks()).isEqualTo(6);
		}

		@Test
		void rapidFireHalvesCooldownToThreeTicks() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.applyPowerUp(state, alice, PowerUpType.RAPID_FIRE);

			gameService.applyInput(state, alice, new PlayerInput(false, false, false, false, true, 1));
			gameService.tick(state);

			assertThat(state.getPlayer(alice).getFireCooldownTicks()).isEqualTo(3);
		}
	}

	@Nested
	class CombatTests {

		@Test
		void playerTakesDamageWhenHit() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			Client bob = new Client("Bob");
			GameState state = gameService.startGame(alice, bob);

			gameService.hitPlayer(state, bob);

			assertThat(state.getPlayer(bob).getHp()).isEqualTo(2);
		}

		@Test
		void playerDiesWhenHpReachesZero() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));

			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);

			assertThat(state.getPlayer(alice).isAlive()).isFalse();
		}

		@Test
		void playerHpDropsToZeroExactlyOnThirdHit() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));

			gameService.hitPlayer(state, alice);
			assertThat(state.getPlayer(alice).getHp()).isEqualTo(2);
			gameService.hitPlayer(state, alice);
			assertThat(state.getPlayer(alice).getHp()).isEqualTo(1);
			gameService.hitPlayer(state, alice);
			assertThat(state.getPlayer(alice).getHp()).isEqualTo(0);
			assertThat(state.getPlayer(alice).isAlive()).isFalse();
		}

		@Test
		void deadPlayerCannotMove() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);
			double deadX = state.getPlayer(alice).getX();

			gameService.applyInput(state, alice, new PlayerInput(false, false, false, true, false, 1));
			gameService.tick(state);

			assertThat(state.getPlayer(alice).getX()).isEqualTo(deadX);
		}

		@Test
		void playerRespawnsAfterNinetyTicks() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);

			for (int i = 0; i < 90; i++) gameService.tick(state);

			assertThat(state.getPlayer(alice).isAlive()).isTrue();
		}

		@Test
		void playerStaysDeadUntilRespawnTicksExpire() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);

			for (int i = 0; i < 50; i++) gameService.tick(state);

			assertThat(state.getPlayer(alice).isAlive()).isFalse();
		}

		@Test
		void playerRespawnsWithFullHp() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);

			for (int i = 0; i < 90; i++) gameService.tick(state);

			assertThat(state.getPlayer(alice).getHp()).isEqualTo(3);
		}

		@Test
		void playerIsInvincibleAfterRespawn() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);
			for (int i = 0; i < 90; i++) gameService.tick(state);

			gameService.hitPlayer(state, alice);

			assertThat(state.getPlayer(alice).getHp()).isEqualTo(3);
		}

		@Test
		void invincibilityWornOffAfterDuration() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);
			for (int i = 0; i < 90; i++) gameService.tick(state);
			for (int i = 0; i < 60; i++) gameService.tick(state);

			gameService.hitPlayer(state, alice);

			assertThat(state.getPlayer(alice).getHp()).isEqualTo(2);
		}

		@Test
		void hitPlayerWhenAlreadyDeadDoesNothing() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);
			int hp = state.getPlayer(alice).getHp();

			gameService.hitPlayer(state, alice);

			assertThat(state.getPlayer(alice).getHp()).isEqualTo(hp);
		}

		@Test
		void hitPlayerWhenInvincibleDoesNothing() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			state.getPlayer(alice).setInvincible(true);

			gameService.hitPlayer(state, alice);

			assertThat(state.getPlayer(alice).getHp()).isEqualTo(3);
		}

		@Test
		void invincibilityTicksSetToSixtyOnRespawn() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);

			for (int i = 0; i < 90; i++) gameService.tick(state);

			assertThat(state.getPlayer(alice).getInvincibilityTicksRemaining()).isEqualTo(60);
			assertThat(state.getPlayer(alice).isInvincible()).isTrue();
		}

		@Test
		void deadPlayerRespawnTicksDecreaseEachTick() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);
			int initialRespawnTicks = state.getPlayer(alice).getRespawnTicksRemaining();

			gameService.tick(state);

			assertThat(state.getPlayer(alice).getRespawnTicksRemaining()).isEqualTo(initialRespawnTicks - 1);
		}
	}

	@Nested
	class WaveTests {

		@Test
		void wave1HasFourScouts() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));

			gameService.spawnWave(state, 1);

			long scouts = state.getEnemies().stream().filter(e -> e.getType().equals("SCOUT")).count();
			assertThat(scouts).isEqualTo(4);
		}

		@Test
		void wave2HasFiveScouts() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));

			gameService.spawnWave(state, 2);

			long scouts = state.getEnemies().stream().filter(e -> e.getType().equals("SCOUT")).count();
			assertThat(scouts).isEqualTo(5);
		}

		@Test
		void wave3HasSixScouts() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));

			gameService.spawnWave(state, 3);

			long scouts = state.getEnemies().stream().filter(e -> e.getType().equals("SCOUT")).count();
			assertThat(scouts).isEqualTo(6);
		}

		@Test
		void wave4HasFiveEnemiesTotal() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));

			gameService.spawnWave(state, 4);

			assertThat(state.getEnemies()).hasSize(5);
		}

		@Test
		void wave5HasThreeScoutsAndTwoFighters() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));

			gameService.spawnWave(state, 5);

			long scouts = state.getEnemies().stream().filter(e -> e.getType().equals("SCOUT")).count();
			long fighters = state.getEnemies().stream().filter(e -> e.getType().equals("FIGHTER")).count();
			assertThat(scouts).isEqualTo(3);
			assertThat(fighters).isEqualTo(2);
		}

		@Test
		void wave6HasThreeScoutsAndTwoFighters() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));

			gameService.spawnWave(state, 6);

			long scouts = state.getEnemies().stream().filter(e -> e.getType().equals("SCOUT")).count();
			long fighters = state.getEnemies().stream().filter(e -> e.getType().equals("FIGHTER")).count();
			assertThat(scouts).isEqualTo(3);
			assertThat(fighters).isEqualTo(2);
		}

		@Test
		void wave7HasSixEnemiesTotal() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));

			gameService.spawnWave(state, 7);

			assertThat(state.getEnemies()).hasSize(6);
		}

		@Test
		void wave8HasScoutsFightersAndBombers() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));

			gameService.spawnWave(state, 8);

			assertThat(state.getEnemies()).anyMatch(e -> e.getType().equals("SCOUT"));
			assertThat(state.getEnemies()).anyMatch(e -> e.getType().equals("FIGHTER"));
			assertThat(state.getEnemies()).anyMatch(e -> e.getType().equals("BOMBER"));
		}

		@Test
		void wave9HasScoutsFightersAndBombers() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));

			gameService.spawnWave(state, 9);

			assertThat(state.getEnemies()).anyMatch(e -> e.getType().equals("SCOUT"));
			assertThat(state.getEnemies()).anyMatch(e -> e.getType().equals("FIGHTER"));
			assertThat(state.getEnemies()).anyMatch(e -> e.getType().equals("BOMBER"));
		}

		@Test
		void wave10HasBossScoutsAndFighters() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));

			gameService.spawnWave(state, 10);

			assertThat(state.getEnemies()).anyMatch(e -> e.getType().equals("BOSS"));
			assertThat(state.getEnemies()).anyMatch(e -> e.getType().equals("SCOUT"));
			assertThat(state.getEnemies()).anyMatch(e -> e.getType().equals("FIGHTER"));
		}
	}

	@Nested
	class EnemyTests {

		@Test
		void waveOneSpawnsOnlyScouts() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));

			gameService.spawnWave(state, 1);

			assertThat(state.getEnemies()).isNotEmpty();
			assertThat(state.getEnemies()).allMatch(e -> e.getType().equals("SCOUT"));
		}

		@Test
		void waveOneHasMultipleEnemies() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));

			gameService.spawnWave(state, 1);

			assertThat(state.getEnemies().size()).isGreaterThan(1);
		}

		@Test
		void scoutHasOneHp() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));
			gameService.spawnWave(state, 1);

			Enemy scout = state.getEnemies().get(0);

			assertThat(scout.getHp()).isEqualTo(1);
		}

		@Test
		void scoutSpawnsAtTopOfField() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));

			gameService.spawnWave(state, 1);

			assertThat(state.getEnemies()).allMatch(e -> e.getY() == 0.0);
		}

		@Test
		void waveFourSpawnsScoutsAndFighters() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));

			gameService.spawnWave(state, 4);

			assertThat(state.getEnemies()).anyMatch(e -> e.getType().equals("SCOUT"));
			assertThat(state.getEnemies()).anyMatch(e -> e.getType().equals("FIGHTER"));
		}

		@Test
		void fighterHasTwoHp() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));
			gameService.spawnWave(state, 4);

			Enemy fighter = state.getEnemies().stream()
				.filter(e -> e.getType().equals("FIGHTER")).findFirst().orElseThrow();

			assertThat(fighter.getHp()).isEqualTo(2);
		}

		@Test
		void waveSevenSpawnsScoutsFightersAndBombers() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));

			gameService.spawnWave(state, 7);

			assertThat(state.getEnemies()).anyMatch(e -> e.getType().equals("SCOUT"));
			assertThat(state.getEnemies()).anyMatch(e -> e.getType().equals("FIGHTER"));
			assertThat(state.getEnemies()).anyMatch(e -> e.getType().equals("BOMBER"));
		}

		@Test
		void bomberHasFourHp() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));
			gameService.spawnWave(state, 7);

			Enemy bomber = state.getEnemies().stream()
				.filter(e -> e.getType().equals("BOMBER")).findFirst().orElseThrow();

			assertThat(bomber.getHp()).isEqualTo(4);
		}

		@Test
		void wavetenSpawnsBoss() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));

			gameService.spawnWave(state, 10);

			assertThat(state.getEnemies()).anyMatch(e -> e.getType().equals("BOSS"));
		}

		@Test
		void bossHasTwentyHp() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));
			gameService.spawnWave(state, 10);

			Enemy boss = state.getEnemies().stream()
				.filter(e -> e.getType().equals("BOSS")).findFirst().orElseThrow();

			assertThat(boss.getHp()).isEqualTo(20);
		}

		@Test
		void spawnWaveClearsPreviousEnemies() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));
			gameService.spawnWave(state, 1);
			int firstCount = state.getEnemies().size();

			gameService.spawnWave(state, 1);

			assertThat(state.getEnemies().size()).isEqualTo(firstCount);
		}

		@Test
		void enemyDiesWhenHpReachesZero() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.spawnWave(state, 1);
			Enemy scout = state.getEnemies().get(0);

			gameService.hitEnemy(state, scout, alice);

			assertThat(state.getEnemies()).doesNotContain(scout);
		}

		@Test
		void enemyNotRemovedWhenStillHasHp() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.spawnWave(state, 4);
			Enemy fighter = state.getEnemies().stream()
				.filter(e -> e.getType().equals("FIGHTER")).findFirst().orElseThrow();

			gameService.hitEnemy(state, fighter, alice);

			assertThat(state.getEnemies()).contains(fighter);
		}

		@Test
		void killingScoutAdds100Points() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.spawnWave(state, 1);
			Enemy scout = state.getEnemies().get(0);

			gameService.hitEnemy(state, scout, alice);

			assertThat(state.getPlayer(alice).getScore()).isEqualTo(100);
		}

		@Test
		void fighterKillAdds250Points() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.spawnWave(state, 4);
			Enemy fighter = state.getEnemies().stream()
				.filter(e -> e.getType().equals("FIGHTER")).findFirst().orElseThrow();

			gameService.hitEnemy(state, fighter, alice);
			gameService.hitEnemy(state, fighter, alice);

			assertThat(state.getPlayer(alice).getScore()).isEqualTo(250);
		}

		@Test
		void bomberKillAdds500Points() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.spawnWave(state, 7);
			Enemy bomber = state.getEnemies().stream()
				.filter(e -> e.getType().equals("BOMBER")).findFirst().orElseThrow();

			for (int i = 0; i < 4; i++) gameService.hitEnemy(state, bomber, alice);

			assertThat(state.getPlayer(alice).getScore()).isEqualTo(500);
		}

		@Test
		void killingBossAdds2000Points() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.spawnWave(state, 10);
			Enemy boss = state.getEnemies().stream()
				.filter(e -> e.getType().equals("BOSS")).findFirst().orElseThrow();

			for (int i = 0; i < 20; i++) gameService.hitEnemy(state, boss, alice);

			assertThat(state.getPlayer(alice).getScore()).isEqualTo(2000);
		}

		@Test
		void bossSurvivesNineteenHits() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.spawnWave(state, 10);
			Enemy boss = state.getEnemies().stream()
				.filter(e -> e.getType().equals("BOSS")).findFirst().orElseThrow();

			for (int i = 0; i < 19; i++) gameService.hitEnemy(state, boss, alice);

			assertThat(state.getEnemies()).contains(boss);
			assertThat(boss.getHp()).isEqualTo(1);
		}

		@Test
		void killingEnemyWithUnknownShooterAddsNoScore() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.spawnWave(state, 1);
			Enemy scout = state.getEnemies().get(0);
			int scoreBefore = state.getPlayer(alice).getScore();

			gameService.hitEnemy(state, scout, new Client("Unknown")); // not in game state

			assertThat(state.getEnemies()).doesNotContain(scout);
			assertThat(state.getPlayer(alice).getScore()).isEqualTo(scoreBefore);
		}
	}

	@Nested
	class PowerUpTests {

		@Test
		void powerUpSpawnsAfterEnemyKill() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));

			for (int i = 0; i < 50; i++) {
				gameService.spawnWave(state, 1);
				gameService.hitEnemy(state, state.getEnemies().get(0), alice);
			}

			assertThat(state.getPowerUps()).isNotEmpty();
		}

		@Test
		void powerUpPositionMatchesEnemyPosition() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));

			for (int i = 0; i < 100; i++) {
				gameService.spawnWave(state, 1);
				Enemy scout = state.getEnemies().get(0);
				double ex = scout.getX();
				double ey = scout.getY();
				gameService.hitEnemy(state, scout, alice);
				if (!state.getPowerUps().isEmpty()) {
					assertThat(state.getPowerUps().get(0).getX()).isEqualTo(ex);
					assertThat(state.getPowerUps().get(0).getY()).isEqualTo(ey);
					return;
				}
			}
		}

		@Test
		void allFourPowerUpTypesExist() {
			assertThat(PowerUpType.values()).containsExactlyInAnyOrder(
				PowerUpType.RAPID_FIRE, PowerUpType.SHIELD,
				PowerUpType.SPEED_BOOST, PowerUpType.HEALTH_PACK);
		}

		@Test
		void shieldAbsorbsOneHit() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.applyPowerUp(state, alice, PowerUpType.SHIELD);

			gameService.hitPlayer(state, alice);

			assertThat(state.getPlayer(alice).getHp()).isEqualTo(3);
		}

		@Test
		void shieldIsConsumedAfterOneHit() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.applyPowerUp(state, alice, PowerUpType.SHIELD);
			gameService.hitPlayer(state, alice);

			gameService.hitPlayer(state, alice);

			assertThat(state.getPlayer(alice).getHp()).isEqualTo(2);
		}

		@Test
		void healthPackRestoresOneHp() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.hitPlayer(state, alice);

			gameService.applyPowerUp(state, alice, PowerUpType.HEALTH_PACK);

			assertThat(state.getPlayer(alice).getHp()).isEqualTo(3);
		}

		@Test
		void healthPackDoesNotExceedMaxHp() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));

			gameService.applyPowerUp(state, alice, PowerUpType.HEALTH_PACK);

			assertThat(state.getPlayer(alice).getHp()).isEqualTo(3);
		}

		@Test
		void rapidFireDoublesFiringRate() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.applyPowerUp(state, alice, PowerUpType.RAPID_FIRE);

			for (int i = 0; i < 12; i++) {
				gameService.applyInput(state, alice, new PlayerInput(false, false, false, false, true, i));
				gameService.tick(state);
			}

			assertThat(state.getProjectiles().size()).isGreaterThan(2);
		}

		@Test
		void rapidFireExpiresAfterDuration() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.applyPowerUp(state, alice, PowerUpType.RAPID_FIRE);

			for (int i = 0; i < 241; i++) gameService.tick(state);

			assertThat(state.getPlayer(alice).getRapidFireTicksRemaining()).isEqualTo(0);
		}

		@Test
		void rapidFireReducesCooldownToThreeTicks() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.applyPowerUp(state, alice, PowerUpType.RAPID_FIRE);

			gameService.applyInput(state, alice, new PlayerInput(false, false, false, false, true, 1));
			gameService.tick(state);

			assertThat(state.getPlayer(alice).getFireCooldownTicks()).isEqualTo(3);
		}

		@Test
		void speedBoostIncreasesMovement() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			Client bob = new Client("Bob");
			GameState state = gameService.startGame(alice, bob);
			double normalStartY = state.getPlayer(alice).getY();
			gameService.applyInput(state, alice, new PlayerInput(true, false, false, false, false, 1));
			gameService.tick(state);
			double normalMove = normalStartY - state.getPlayer(alice).getY();

			GameState state2 = gameService.startGame(alice, bob);
			gameService.applyPowerUp(state2, alice, PowerUpType.SPEED_BOOST);
			double boostedStartY = state2.getPlayer(alice).getY();
			gameService.applyInput(state2, alice, new PlayerInput(true, false, false, false, false, 1));
			gameService.tick(state2);
			double boostedMove = boostedStartY - state2.getPlayer(alice).getY();

			assertThat(boostedMove).isGreaterThan(normalMove);
		}

		@Test
		void speedBoostExpiresAfterDuration() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			gameService.applyPowerUp(state, alice, PowerUpType.SPEED_BOOST);

			for (int i = 0; i < 181; i++) gameService.tick(state);

			assertThat(state.getPlayer(alice).getSpeedBoostTicksRemaining()).isEqualTo(0);
		}

		@Test
		void shieldAbsorbedHitInFfaDoesNotGiveShooterScore() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			Client bob = new Client("Bob");
			GameState state = gameService.startGame(alice, bob);
			gameService.applyPowerUp(state, bob, PowerUpType.SHIELD);

			gameService.hitPlayerByPlayer(state, bob, alice); // Shield absorbs

			assertThat(state.getPlayer(alice).getScore()).isEqualTo(0);
			assertThat(state.getPlayer(bob).isShielded()).isFalse();
		}
	}

	@Nested
	class FfaScoringTests {

		@Test
		void ffaHittingPlayerAddsHundredPoints() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			Client bob = new Client("Bob");
			GameState state = gameService.startGame(alice, bob);

			gameService.hitPlayerByPlayer(state, bob, alice);

			assertThat(state.getPlayer(alice).getScore()).isEqualTo(100);
		}

		@Test
		void ffaKillingPlayerAddsBonus() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			Client bob = new Client("Bob");
			GameState state = gameService.startGame(alice, bob);

			gameService.hitPlayerByPlayer(state, bob, alice);
			gameService.hitPlayerByPlayer(state, bob, alice);
			gameService.hitPlayerByPlayer(state, bob, alice);

			assertThat(state.getPlayer(alice).getScore()).isEqualTo(100 + 100 + 100 + 500);
		}

		@Test
		void ffaDeathDeductsScore() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			Client bob = new Client("Bob");
			GameState state = gameService.startGame(alice, bob);

			gameService.hitPlayerByPlayer(state, alice, bob);
			gameService.hitPlayerByPlayer(state, alice, bob);
			gameService.hitPlayerByPlayer(state, alice, bob);

			assertThat(state.getPlayer(alice).getScore()).isEqualTo(-200);
		}

		@Test
		void ffaScoreCanBeNegative() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));

			gameService.hitPlayerByPlayer(state, alice, new Client("Stranger"));
			gameService.hitPlayerByPlayer(state, alice, new Client("Stranger"));
			gameService.hitPlayerByPlayer(state, alice, new Client("Stranger"));

			assertThat(state.getPlayer(alice).getScore()).isLessThan(0);
		}

		@Test
		void ffaHittingDeadPlayerDoesNotAddScore() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			Client bob = new Client("Bob");
			GameState state = gameService.startGame(alice, bob);
			gameService.hitPlayerByPlayer(state, bob, alice);
			gameService.hitPlayerByPlayer(state, bob, alice);
			gameService.hitPlayerByPlayer(state, bob, alice);
			int scoreAfterKill = state.getPlayer(alice).getScore();

			gameService.hitPlayerByPlayer(state, bob, alice);

			assertThat(state.getPlayer(alice).getScore()).isEqualTo(scoreAfterKill);
		}

		@Test
		void invinciblePlayerNotDamagedByOtherPlayer() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			Client bob = new Client("Bob");
			GameState state = gameService.startGame(alice, bob);
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);
			gameService.hitPlayer(state, alice);
			for (int i = 0; i < 90; i++) gameService.tick(state);

			gameService.hitPlayerByPlayer(state, alice, bob);

			assertThat(state.getPlayer(alice).getHp()).isEqualTo(3);
		}

		@Test
		void ffaGameOverWhenScoreReached() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			state.getPlayer(alice).setScore(GameService.FFA_SCORE_LIMIT);

			assertThat(gameService.isFfaGameOver(state)).isTrue();
		}

		@Test
		void ffaGameNotOverBelowScoreLimit() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));

			assertThat(gameService.isFfaGameOver(state)).isFalse();
		}

		@Test
		void ffaGameNotOverAtScoreLimitMinusOne() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			GameState state = gameService.startGame(alice, new Client("Bob"));
			state.getPlayer(alice).setScore(GameService.FFA_SCORE_LIMIT - 1);

			assertThat(gameService.isFfaGameOver(state)).isFalse();
		}

		@Test
		void ffaGameOverWhenTimeLimitReached() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));
			state.setTick(GameService.FFA_TICK_LIMIT);

			assertThat(gameService.isFfaGameOver(state)).isTrue();
		}

		@Test
		void ffaGameNotOverAtTickLimitMinusOne() {
			GameService gameService = new GameService();
			GameState state = gameService.startGame(new Client("Alice"), new Client("Bob"));
			state.setTick(GameService.FFA_TICK_LIMIT - 1);

			assertThat(gameService.isFfaGameOver(state)).isFalse();
		}

		@Test
		void ffaShieldedPlayerHitDoesNotGiveShooterScore() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			Client bob = new Client("Bob");
			GameState state = gameService.startGame(alice, bob);
			gameService.applyPowerUp(state, bob, PowerUpType.SHIELD);

			gameService.hitPlayerByPlayer(state, bob, alice);

			assertThat(state.getPlayer(alice).getScore()).isEqualTo(0);
		}

		@Test
		void ffaTwoShootersCanBothScore() {
			GameService gameService = new GameService();
			Client alice = new Client("Alice");
			Client bob = new Client("Bob");
			Client carol = new Client("Carol");
			GameState state = gameService.startGame(alice, bob, carol);

			gameService.hitPlayerByPlayer(state, carol, alice);
			gameService.hitPlayerByPlayer(state, carol, bob);

			assertThat(state.getPlayer(alice).getScore()).isEqualTo(100);
			assertThat(state.getPlayer(bob).getScore()).isEqualTo(100);
		}
	}
}
