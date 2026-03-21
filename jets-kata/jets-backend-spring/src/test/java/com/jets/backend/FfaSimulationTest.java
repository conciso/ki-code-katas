package com.jets.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.jets.backend.game.Enemy;
import com.jets.backend.game.GameService;
import com.jets.backend.game.GameState;
import com.jets.backend.game.PlayerInput;
import com.jets.backend.game.PowerUpType;
import com.jets.backend.lobby.Lobby;
import com.jets.backend.lobby.LobbyService;
import com.jets.backend.model.Client;

class FfaSimulationTest {

	/**
	 * Der ultimative FFA End-to-End Simulationstest.
	 *
	 * Simuliert eine vollständige FFA-Spielsession mit 4 Spielern:
	 *
	 * LOBBY:
	 *   - Lobby erstellen, 3 Spieler joinen → voll
	 *   - Modus explizit auf FFA setzen (kein Co-Op!)
	 *   - Nur Host darf Modus ändern
	 *   - Alle Spieler bereit, Host startet
	 *   - Beitreten im laufenden Spiel verboten
	 *
	 * FFA SPIELPHASE — alle 4 Spieler kämpfen gegeneinander:
	 *   - Jeder Treffer gibt dem Schützen 100 Punkte
	 *   - Jeder Kill gibt 500 Bonuspunkte
	 *   - Tod kostet 200 Punkte
	 *   - Respawn nach 90 Ticks mit vollem HP
	 *   - 60 Ticks Unverwundbarkeit nach Respawn
	 *
	 * POWER-UPS (alle 4 Typen):
	 *   - Shield absorbiert exakt 1 Treffer
	 *   - Health Pack heilt 1 HP (nicht über 3)
	 *   - Speed Boost erhöht Bewegungsgeschwindigkeit
	 *   - Rapid Fire verdoppelt Feuerrate
	 *
	 * WELLEN (parallel zum FFA):
	 *   - Welle 1: nur Scouts (1 HP, 100 Punkte)
	 *   - Welle 4: Scouts + Fighters (2 HP, 250 Punkte)
	 *   - Welle 7: Scouts + Fighters + Bombers (4 HP, 500 Punkte)
	 *   - Welle 10: Boss (20 HP, 2000 Punkte) + gemischt
	 *
	 * FFA GAME OVER:
	 *   - Score-Limit erreicht → isFfaGameOver = true
	 *   - Zeit-Limit erreicht → isFfaGameOver = true
	 *
	 * RETURN TO LOBBY:
	 *   - Nur Host darf zurück
	 *   - Alle Spieler werden un-ready
	 *   - inProgress wird auf false gesetzt
	 *
	 * ZWEITE RUNDE:
	 *   - Neue Spielsession startet korrekt
	 *   - Scores beginnen wieder bei 0
	 */
	@Test
	void theUltimateFFAGameSimulation() {
		LobbyService lobbyService = new LobbyService();
		GameService gameService = new GameService();

		Client alice = new Client("Alice");
		Client bob   = new Client("Bob");
		Client carol = new Client("Carol");
		Client dave  = new Client("Dave");

		// =========================================================
		// LOBBY PHASE
		// =========================================================

		Lobby lobby = lobbyService.createLobby(alice);
		assertThat(lobby.getCode()).matches("[A-Z0-9]{6}");
		assertThat(lobby.getHost()).isEqualTo(alice);
		assertThat(lobby.getGameMode()).isEqualTo("COOP"); // Standard ist COOP

		lobbyService.joinLobby(lobby.getCode(), bob);
		lobbyService.joinLobby(lobby.getCode(), carol);
		lobbyService.joinLobby(lobby.getCode(), dave);
		assertThat(lobby.getNumberPlayers()).isEqualTo(4);

		// Lobby ist voll
		assertThatThrownBy(() -> lobbyService.joinLobby(lobby.getCode(), new Client("Eve")))
			.isInstanceOf(GameException.class)
			.hasFieldOrPropertyWithValue("errorCode", "LOBBY_FULL");

		// FFA aktivieren — nur Host darf das
		assertThatThrownBy(() -> lobbyService.setGameMode(lobby.getCode(), bob, "FFA"))
			.isInstanceOf(GameException.class)
			.hasFieldOrPropertyWithValue("errorCode", "NOT_HOST");
		lobbyService.setGameMode(lobby.getCode(), alice, "FFA");
		assertThat(lobby.getGameMode()).isEqualTo("FFA"); // ← explizit FFA, kein Co-Op

		// Nicht alle ready → kein Start
		lobbyService.setReady(lobby.getCode(), alice, true);
		lobbyService.setReady(lobby.getCode(), bob,   true);
		lobbyService.setReady(lobby.getCode(), carol, true);
		assertThatThrownBy(() -> lobbyService.startGame(lobby.getCode(), alice))
			.isInstanceOf(GameException.class)
			.hasFieldOrPropertyWithValue("errorCode", "NOT_ALL_READY");

		// Nicht-Host kann nicht starten
		lobbyService.setReady(lobby.getCode(), dave, true);
		assertThatThrownBy(() -> lobbyService.startGame(lobby.getCode(), dave))
			.isInstanceOf(GameException.class)
			.hasFieldOrPropertyWithValue("errorCode", "NOT_HOST");

		lobbyService.startGame(lobby.getCode(), alice);
		assertThat(lobby.isInProgress()).isTrue();

		// Beitreten verboten während Spiel läuft
		assertThatThrownBy(() -> lobbyService.joinLobby(lobby.getCode(), new Client("Late")))
			.isInstanceOf(GameException.class)
			.hasFieldOrPropertyWithValue("errorCode", "GAME_IN_PROGRESS");

		// =========================================================
		// GAME START — FFA mit 4 Spielern
		// =========================================================

		GameState state = gameService.startGame(alice, bob, carol, dave);

		// Alle Spieler starten mit 3 HP, alive, Score 0
		for (Client p : new Client[]{alice, bob, carol, dave}) {
			assertThat(state.getPlayer(p).getHp()).isEqualTo(3);
			assertThat(state.getPlayer(p).isAlive()).isTrue();
			assertThat(state.getPlayer(p).getScore()).isEqualTo(0);
		}

		// Alle starten an unterschiedlichen X-Positionen
		assertThat(state.getPlayer(alice).getX()).isNotEqualTo(state.getPlayer(bob).getX());
		assertThat(state.getPlayer(bob).getX()).isNotEqualTo(state.getPlayer(carol).getX());
		assertThat(state.getPlayer(carol).getX()).isNotEqualTo(state.getPlayer(dave).getX());

		// Game Over noch nicht erreicht
		assertThat(gameService.isFfaGameOver(state)).isFalse();

		// =========================================================
		// FFA COMBAT: Alice vs Bob
		// =========================================================

		// Treffer 1 → +100 für Alice, Bob hat noch 2 HP
		gameService.hitPlayerByPlayer(state, bob, alice);
		assertThat(state.getPlayer(alice).getScore()).isEqualTo(100);
		assertThat(state.getPlayer(bob).getHp()).isEqualTo(2);

		// Treffer 2 → Alice insgesamt +200
		gameService.hitPlayerByPlayer(state, bob, alice);
		assertThat(state.getPlayer(alice).getScore()).isEqualTo(200);
		assertThat(state.getPlayer(bob).getHp()).isEqualTo(1);

		// Kill-Schuss → +100 Treffer + 500 Kill = +600 für Alice
		// Bob bekommt -200 für den Tod
		gameService.hitPlayerByPlayer(state, bob, alice);
		assertThat(state.getPlayer(bob).isAlive()).isFalse();
		assertThat(state.getPlayer(alice).getScore()).isEqualTo(800); // 200 + 100 + 500
		assertThat(state.getPlayer(bob).getScore()).isEqualTo(-200);

		// Toter Bob kann nicht nochmal Schaden nehmen (Score bleibt)
		gameService.hitPlayerByPlayer(state, bob, carol);
		assertThat(state.getPlayer(carol).getScore()).isEqualTo(0);
		assertThat(state.getPlayer(bob).getScore()).isEqualTo(-200);

		// Bob respawnt nach exakt 90 Ticks
		for (int t = 0; t < 89; t++) gameService.tick(state);
		assertThat(state.getPlayer(bob).isAlive()).isFalse();
		gameService.tick(state); // Tick 90
		assertThat(state.getPlayer(bob).isAlive()).isTrue();
		assertThat(state.getPlayer(bob).getHp()).isEqualTo(3);
		assertThat(state.getPlayer(bob).isInvincible()).isTrue();

		// Treffer während Unverwundbarkeit → kein Schaden, kein Score für Schützen
		int carolScoreBeforeInvHit = state.getPlayer(carol).getScore();
		gameService.hitPlayerByPlayer(state, bob, carol);
		assertThat(state.getPlayer(bob).getHp()).isEqualTo(3);
		assertThat(state.getPlayer(carol).getScore()).isEqualTo(carolScoreBeforeInvHit);

		// Unverwundbarkeit endet nach 60 Ticks
		for (int t = 0; t < 60; t++) gameService.tick(state);
		assertThat(state.getPlayer(bob).isInvincible()).isFalse();

		// Jetzt trifft Treffer wieder
		gameService.hitPlayerByPlayer(state, bob, carol);
		assertThat(state.getPlayer(bob).getHp()).isEqualTo(2);

		// =========================================================
		// FFA COMBAT: Carol vs Dave (kompetitives Duell)
		// =========================================================

		gameService.hitPlayerByPlayer(state, dave, carol);
		gameService.hitPlayerByPlayer(state, carol, dave);
		assertThat(state.getPlayer(carol).getScore()).isGreaterThan(carolScoreBeforeInvHit);
		assertThat(state.getPlayer(dave).getHp()).isEqualTo(2);
		assertThat(state.getPlayer(carol).getHp()).isEqualTo(2);

		// =========================================================
		// POWER-UPS
		// =========================================================

		// Shield absorbiert exakt 1 Treffer
		gameService.applyPowerUp(state, dave, PowerUpType.SHIELD);
		gameService.hitPlayerByPlayer(state, dave, alice); // Shield absorbiert
		assertThat(state.getPlayer(dave).getHp()).isEqualTo(2); // kein HP-Verlust
		assertThat(state.getPlayer(dave).isShielded()).isFalse(); // Shield verbraucht
		gameService.hitPlayerByPlayer(state, dave, alice); // jetzt echter Treffer
		assertThat(state.getPlayer(dave).getHp()).isEqualTo(1);

		// Health Pack heilt 1 HP (max 3)
		gameService.applyPowerUp(state, dave, PowerUpType.HEALTH_PACK);
		assertThat(state.getPlayer(dave).getHp()).isEqualTo(2);
		gameService.applyPowerUp(state, dave, PowerUpType.HEALTH_PACK);
		assertThat(state.getPlayer(dave).getHp()).isEqualTo(3);
		gameService.applyPowerUp(state, dave, PowerUpType.HEALTH_PACK); // schon voll
		assertThat(state.getPlayer(dave).getHp()).isEqualTo(3); // kein Overflow

		// Speed Boost erhöht Bewegungsgeschwindigkeit
		double normalStartY = state.getPlayer(carol).getY();
		gameService.applyInput(state, carol, new PlayerInput(true, false, false, false, false, 1));
		gameService.tick(state);
		double normalMoveY = normalStartY - state.getPlayer(carol).getY();

		GameState state2 = gameService.startGame(alice, bob, carol, dave);
		gameService.applyPowerUp(state2, carol, PowerUpType.SPEED_BOOST);
		double boostedStartY = state2.getPlayer(carol).getY();
		gameService.applyInput(state2, carol, new PlayerInput(true, false, false, false, false, 1));
		gameService.tick(state2);
		double boostedMoveY = boostedStartY - state2.getPlayer(carol).getY();
		assertThat(boostedMoveY).isGreaterThan(normalMoveY);

		// Rapid Fire: in 12 Ticks mehr als 2 Schüsse möglich (normal: max 2)
		gameService.applyPowerUp(state, alice, PowerUpType.RAPID_FIRE);
		assertThat(state.getPlayer(alice).getRapidFireTicksRemaining()).isGreaterThan(0);
		for (int i = 0; i < 12; i++) {
			gameService.applyInput(state, alice, new PlayerInput(false, false, false, false, true, i));
			gameService.tick(state);
		}
		long aliceProjectiles = state.getProjectiles().stream()
			.filter(p -> p.getOwner().equals(alice)).count();
		assertThat(aliceProjectiles).isGreaterThan(2);

		// =========================================================
		// WELLEN
		// =========================================================

		// Welle 1: nur Scouts
		gameService.spawnWave(state, 1);
		assertThat(state.getEnemies()).isNotEmpty();
		assertThat(state.getEnemies()).allMatch(e -> e.getType().equals("SCOUT"));
		assertThat(state.getEnemies().get(0).getHp()).isEqualTo(1);

		int scoreBeforeWave1 = state.getPlayer(bob).getScore();
		Enemy scout1 = state.getEnemies().get(0);
		gameService.hitEnemy(state, scout1, bob);
		assertThat(state.getEnemies()).doesNotContain(scout1);
		assertThat(state.getPlayer(bob).getScore()).isEqualTo(scoreBeforeWave1 + 100);

		// Welle 4: Scouts + Fighters
		gameService.spawnWave(state, 4);
		assertThat(state.getEnemies()).anyMatch(e -> e.getType().equals("SCOUT"));
		assertThat(state.getEnemies()).anyMatch(e -> e.getType().equals("FIGHTER"));
		Enemy fighter = state.getEnemies().stream()
			.filter(e -> e.getType().equals("FIGHTER")).findFirst().orElseThrow();
		assertThat(fighter.getHp()).isEqualTo(2);
		gameService.hitEnemy(state, fighter, carol);
		assertThat(state.getEnemies()).contains(fighter); // 1 HP übrig
		gameService.hitEnemy(state, fighter, carol);
		assertThat(state.getEnemies()).doesNotContain(fighter);
		assertThat(state.getPlayer(carol).getScore()).isGreaterThanOrEqualTo(250);

		// Welle 7: Scouts + Fighters + Bombers
		gameService.spawnWave(state, 7);
		assertThat(state.getEnemies()).anyMatch(e -> e.getType().equals("BOMBER"));
		Enemy bomber = state.getEnemies().stream()
			.filter(e -> e.getType().equals("BOMBER")).findFirst().orElseThrow();
		assertThat(bomber.getHp()).isEqualTo(4);
		for (int i = 0; i < 3; i++) gameService.hitEnemy(state, bomber, dave);
		assertThat(state.getEnemies()).contains(bomber); // noch nicht tot
		gameService.hitEnemy(state, bomber, dave);
		assertThat(state.getEnemies()).doesNotContain(bomber);

		// Welle 10: Boss + gemischt
		gameService.spawnWave(state, 10);
		assertThat(state.getEnemies()).anyMatch(e -> e.getType().equals("BOSS"));
		assertThat(state.getEnemies()).anyMatch(e -> e.getType().equals("SCOUT"));
		Enemy boss = state.getEnemies().stream()
			.filter(e -> e.getType().equals("BOSS")).findFirst().orElseThrow();
		assertThat(boss.getHp()).isEqualTo(20);
		int scoreBeforeBoss = state.getPlayer(alice).getScore();
		for (int i = 0; i < 19; i++) gameService.hitEnemy(state, boss, alice);
		assertThat(state.getEnemies()).contains(boss); // 1 HP übrig
		gameService.hitEnemy(state, boss, alice);
		assertThat(state.getEnemies()).doesNotContain(boss);
		assertThat(state.getPlayer(alice).getScore()).isEqualTo(scoreBeforeBoss + 2000);

		// =========================================================
		// FFA GAME OVER
		// =========================================================

		assertThat(gameService.isFfaGameOver(state)).isFalse();

		// Score-Limit
		state.getPlayer(alice).setScore(GameService.FFA_SCORE_LIMIT);
		assertThat(gameService.isFfaGameOver(state)).isTrue();

		// Zeit-Limit (separates State-Objekt)
		GameState timeState = gameService.startGame(alice, bob);
		assertThat(gameService.isFfaGameOver(timeState)).isFalse();
		timeState.setTick(GameService.FFA_TICK_LIMIT);
		assertThat(gameService.isFfaGameOver(timeState)).isTrue();

		// =========================================================
		// RETURN TO LOBBY
		// =========================================================

		assertThatThrownBy(() -> lobbyService.returnToLobby(lobby.getCode(), bob))
			.isInstanceOf(GameException.class)
			.hasFieldOrPropertyWithValue("errorCode", "NOT_HOST");

		lobbyService.returnToLobby(lobby.getCode(), alice);
		assertThat(lobby.isInProgress()).isFalse();
		assertThat(lobby.getGameMode()).isEqualTo("FFA"); // Modus bleibt erhalten
		for (Client p : new Client[]{alice, bob, carol, dave}) {
			assertThat(p.isReady()).isFalse();
		}

		// =========================================================
		// ZWEITE RUNDE — neues Spiel startet korrekt
		// =========================================================

		// Spieler müssen sich erneut ready setzen
		assertThatThrownBy(() -> lobbyService.startGame(lobby.getCode(), alice))
			.isInstanceOf(GameException.class)
			.hasFieldOrPropertyWithValue("errorCode", "NOT_ALL_READY");

		lobbyService.setReady(lobby.getCode(), alice, true);
		lobbyService.setReady(lobby.getCode(), bob,   true);
		lobbyService.setReady(lobby.getCode(), carol, true);
		lobbyService.setReady(lobby.getCode(), dave,  true);
		lobbyService.startGame(lobby.getCode(), alice);
		assertThat(lobby.isInProgress()).isTrue();

		GameState round2 = gameService.startGame(alice, bob, carol, dave);
		for (Client p : new Client[]{alice, bob, carol, dave}) {
			assertThat(round2.getPlayer(p).getHp()).isEqualTo(3);
			assertThat(round2.getPlayer(p).isAlive()).isTrue();
			assertThat(round2.getPlayer(p).getScore()).isEqualTo(0);
		}
		assertThat(gameService.isFfaGameOver(round2)).isFalse();
	}
}
