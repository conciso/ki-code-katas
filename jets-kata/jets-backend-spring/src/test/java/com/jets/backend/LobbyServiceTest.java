package com.jets.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.jets.backend.lobby.Lobby;
import com.jets.backend.lobby.LobbyService;
import com.jets.backend.model.Client;

class LobbyServiceTest {

	@Test
	void createLobbyReturnsSixCharacterCode() {
		LobbyService lobbyService = new LobbyService();
		Client alice = new Client();

		Lobby lobby = lobbyService.createLobby(alice);

		assertThat(lobby.getCode()).hasSize(6);
	}

	@Test
	void clientHasName() {
		Client alice = new Client("Alice");

		assertThat(alice.getName()).isEqualTo("Alice");
	}

	@Test
	void createdLobbyContainsCreator() {
		LobbyService lobbyService = new LobbyService();
		Client alice = new Client("Alice");

		Lobby lobby = lobbyService.createLobby(alice);

		assertThat(lobby.getNumberPlayers()).isEqualTo(1);
	}

	@Test
	void creatorIsHost() {
		LobbyService lobbyService = new LobbyService();
		Client alice = new Client("Alice");

		Lobby lobby = lobbyService.createLobby(alice);

		assertThat(lobby.getHost()).isEqualTo(alice);
	}

	@Test
	void playerCanJoinLobby() {
		LobbyService lobbyService = new LobbyService();
		Lobby lobby = lobbyService.createLobby(new Client("Alice"));
		Client bob = new Client("Bob");

		lobbyService.joinLobby(lobby.getCode(), bob);

		assertThat(lobby.getNumberPlayers()).isEqualTo(2);
	}

	@Test
	void joinLobbyWithUnknownCodeThrows() {
		LobbyService lobbyService = new LobbyService();

		assertThatThrownBy(() -> lobbyService.joinLobby("XXXXXX", new Client("Bob")))
			.isInstanceOf(GameException.class)
			.hasFieldOrPropertyWithValue("errorCode", "LOBBY_NOT_FOUND");
	}

	@Test
	void lobbyIsFullAfterFourPlayers() {
		LobbyService lobbyService = new LobbyService();
		Lobby lobby = lobbyService.createLobby(new Client("Alice"));
		lobbyService.joinLobby(lobby.getCode(), new Client("Bob"));
		lobbyService.joinLobby(lobby.getCode(), new Client("Charlie"));
		lobbyService.joinLobby(lobby.getCode(), new Client("Dave"));

		assertThatThrownBy(() -> lobbyService.joinLobby(lobby.getCode(), new Client("Eve")))
			.isInstanceOf(GameException.class)
			.hasFieldOrPropertyWithValue("errorCode", "LOBBY_FULL");
	}

	@Test
	void lobbyHasFourPlayersAfterThreeJoin() {
		LobbyService lobbyService = new LobbyService();
		Client alice = new Client("Alice");
		Lobby lobby = lobbyService.createLobby(alice);
		lobbyService.joinLobby(lobby.getCode(), new Client("Bob"));
		lobbyService.joinLobby(lobby.getCode(), new Client("Charlie"));
		lobbyService.joinLobby(lobby.getCode(), new Client("Dave"));

		assertThat(lobby.getNumberPlayers()).isEqualTo(4);
	}

	@Test
	void playerCanSetReady() {
		LobbyService lobbyService = new LobbyService();
		Client alice = new Client("Alice");
		Lobby lobby = lobbyService.createLobby(alice);

		lobbyService.setReady(lobby.getCode(), alice, true);

		assertThat(alice.isReady()).isTrue();
	}

	@Test
	void playerCanUnready() {
		LobbyService lobbyService = new LobbyService();
		Client alice = new Client("Alice");
		Lobby lobby = lobbyService.createLobby(alice);
		lobbyService.setReady(lobby.getCode(), alice, true);

		lobbyService.setReady(lobby.getCode(), alice, false);

		assertThat(alice.isReady()).isFalse();
	}

	@Test
	void onlyHostCanStartGame() {
		LobbyService lobbyService = new LobbyService();
		Client alice = new Client("Alice");
		Client bob = new Client("Bob");
		Lobby lobby = lobbyService.createLobby(alice);
		lobbyService.joinLobby(lobby.getCode(), bob);

		assertThatThrownBy(() -> lobbyService.startGame(lobby.getCode(), bob))
			.isInstanceOf(GameException.class)
			.hasFieldOrPropertyWithValue("errorCode", "NOT_HOST");
	}

	@Test
	void gameRequiresAtLeastTwoPlayers() {
		LobbyService lobbyService = new LobbyService();
		Client alice = new Client("Alice");
		Lobby lobby = lobbyService.createLobby(alice);
		lobbyService.setReady(lobby.getCode(), alice, true);

		assertThatThrownBy(() -> lobbyService.startGame(lobby.getCode(), alice))
			.isInstanceOf(GameException.class)
			.hasFieldOrPropertyWithValue("errorCode", "NOT_ENOUGH_PLAYERS");
	}

	@Test
	void gameRequiresAllPlayersReady() {
		LobbyService lobbyService = new LobbyService();
		Client alice = new Client("Alice");
		Client bob = new Client("Bob");
		Lobby lobby = lobbyService.createLobby(alice);
		lobbyService.joinLobby(lobby.getCode(), bob);
		lobbyService.setReady(lobby.getCode(), alice, true);

		assertThatThrownBy(() -> lobbyService.startGame(lobby.getCode(), alice))
			.isInstanceOf(GameException.class)
			.hasFieldOrPropertyWithValue("errorCode", "NOT_ALL_READY");
	}

	@Test
	void cannotJoinLobbyInProgress() {
		LobbyService lobbyService = new LobbyService();
		Client alice = new Client("Alice");
		Client bob = new Client("Bob");
		Lobby lobby = lobbyService.createLobby(alice);
		lobbyService.joinLobby(lobby.getCode(), bob);
		lobbyService.setReady(lobby.getCode(), alice, true);
		lobbyService.setReady(lobby.getCode(), bob, true);
		lobbyService.startGame(lobby.getCode(), alice);

		assertThatThrownBy(() -> lobbyService.joinLobby(lobby.getCode(), new Client("Charlie")))
			.isInstanceOf(GameException.class)
			.hasFieldOrPropertyWithValue("errorCode", "GAME_IN_PROGRESS");
	}

	@Test
	void hostChangesWhenHostLeaves() {
		LobbyService lobbyService = new LobbyService();
		Client alice = new Client("Alice");
		Client bob = new Client("Bob");
		Lobby lobby = lobbyService.createLobby(alice);
		lobbyService.joinLobby(lobby.getCode(), bob);

		lobbyService.leaveLobby(lobby.getCode(), alice);

		assertThat(lobby.getHost()).isEqualTo(bob);
	}

	@Test
	void lobbyIsEmptyAfterLastPlayerLeaves() {
		LobbyService lobbyService = new LobbyService();
		Client alice = new Client("Alice");
		Lobby lobby = lobbyService.createLobby(alice);

		lobbyService.leaveLobby(lobby.getCode(), alice);

		assertThat(lobby.getNumberPlayers()).isEqualTo(0);
	}

	@Test
	void hostCanSetGameMode() {
		LobbyService lobbyService = new LobbyService();
		Client alice = new Client("Alice");
		Lobby lobby = lobbyService.createLobby(alice);

		lobbyService.setGameMode(lobby.getCode(), alice, "FFA");

		assertThat(lobby.getGameMode()).isEqualTo("FFA");
	}

	@Test
	void onlyHostCanSetGameMode() {
		LobbyService lobbyService = new LobbyService();
		Client alice = new Client("Alice");
		Client bob = new Client("Bob");
		Lobby lobby = lobbyService.createLobby(alice);
		lobbyService.joinLobby(lobby.getCode(), bob);

		assertThatThrownBy(() -> lobbyService.setGameMode(lobby.getCode(), bob, "FFA"))
			.isInstanceOf(GameException.class)
			.hasFieldOrPropertyWithValue("errorCode", "NOT_HOST");
	}

	@Test
	void gameModeCanBeSetToFFA() {
		LobbyService lobbyService = new LobbyService();
		Client alice = new Client("Alice");
		Lobby lobby = lobbyService.createLobby(alice);

		lobbyService.setGameMode(lobby.getCode(), alice, "FFA");

		assertThat(lobby.getGameMode()).isEqualTo("FFA");
	}

	@Test
	void lobbyDefaultGameModeIsCoop() {
		LobbyService lobbyService = new LobbyService();
		Lobby lobby = lobbyService.createLobby(new Client("Alice"));

		assertThat(lobby.getGameMode()).isEqualTo("COOP");
	}

	@Test
	void returnToLobbyResetsInProgressState() {
		LobbyService lobbyService = new LobbyService();
		Client alice = new Client("Alice");
		Client bob = new Client("Bob");
		Lobby lobby = lobbyService.createLobby(alice);
		lobbyService.joinLobby(lobby.getCode(), bob);
		lobbyService.setReady(lobby.getCode(), alice, true);
		lobbyService.setReady(lobby.getCode(), bob, true);
		lobbyService.startGame(lobby.getCode(), alice);

		lobbyService.returnToLobby(lobby.getCode(), alice);

		assertThat(lobby.isInProgress()).isFalse();
	}

	@Test
	void returnToLobbyResetsReadyState() {
		LobbyService lobbyService = new LobbyService();
		Client alice = new Client("Alice");
		Client bob = new Client("Bob");
		Lobby lobby = lobbyService.createLobby(alice);
		lobbyService.joinLobby(lobby.getCode(), bob);
		lobbyService.setReady(lobby.getCode(), alice, true);
		lobbyService.setReady(lobby.getCode(), bob, true);
		lobbyService.startGame(lobby.getCode(), alice);

		lobbyService.returnToLobby(lobby.getCode(), alice);

		assertThat(alice.isReady()).isFalse();
		assertThat(bob.isReady()).isFalse();
	}

	@Test
	void onlyHostCanReturnToLobby() {
		LobbyService lobbyService = new LobbyService();
		Client alice = new Client("Alice");
		Client bob = new Client("Bob");
		Lobby lobby = lobbyService.createLobby(alice);
		lobbyService.joinLobby(lobby.getCode(), bob);
		lobbyService.setReady(lobby.getCode(), alice, true);
		lobbyService.setReady(lobby.getCode(), bob, true);
		lobbyService.startGame(lobby.getCode(), alice);

		assertThatThrownBy(() -> lobbyService.returnToLobby(lobby.getCode(), bob))
			.isInstanceOf(GameException.class)
			.hasFieldOrPropertyWithValue("errorCode", "NOT_HOST");
	}

	@Test
	void lobbyCodesAreUnique() {
		LobbyService lobbyService = new LobbyService();

		String code1 = lobbyService.createLobby(new Client("Alice")).getCode();
		String code2 = lobbyService.createLobby(new Client("Bob")).getCode();

		assertThat(code1).isNotEqualTo(code2);
	}

	@Test
	void lobbyCodeContainsOnlyValidChars() {
		LobbyService lobbyService = new LobbyService();

		for (int i = 0; i < 20; i++) {
			String code = lobbyService.createLobby(new Client("Player")).getCode();
			assertThat(code).matches("[A-Z0-9]{6}");
		}
	}

	// --- Fehlerbehandlung: ungültige Codes ---

	@Test
	void multipleLobbiesCanCoexist() {
		LobbyService lobbyService = new LobbyService();
		Lobby lobby1 = lobbyService.createLobby(new Client("Alice"));
		Lobby lobby2 = lobbyService.createLobby(new Client("Bob"));

		lobbyService.joinLobby(lobby1.getCode(), new Client("C1"));
		lobbyService.joinLobby(lobby2.getCode(), new Client("C2"));

		assertThat(lobby1.getNumberPlayers()).isEqualTo(2);
		assertThat(lobby2.getNumberPlayers()).isEqualTo(2);
	}

	@Test
	void setReadyWithInvalidCodeThrows() {
		LobbyService lobbyService = new LobbyService();

		assertThatThrownBy(() -> lobbyService.setReady("XXXXXX", new Client("Alice"), true))
			.isInstanceOf(GameException.class)
			.hasFieldOrPropertyWithValue("errorCode", "LOBBY_NOT_FOUND");
	}

	@Test
	void leaveLobbyWithInvalidCodeThrows() {
		LobbyService lobbyService = new LobbyService();

		assertThatThrownBy(() -> lobbyService.leaveLobby("XXXXXX", new Client("Alice")))
			.isInstanceOf(GameException.class)
			.hasFieldOrPropertyWithValue("errorCode", "LOBBY_NOT_FOUND");
	}

	@Test
	void setGameModeWithInvalidCodeThrows() {
		LobbyService lobbyService = new LobbyService();

		assertThatThrownBy(() -> lobbyService.setGameMode("XXXXXX", new Client("Alice"), "FFA"))
			.isInstanceOf(GameException.class)
			.hasFieldOrPropertyWithValue("errorCode", "LOBBY_NOT_FOUND");
	}

	@Test
	void returnToLobbyWithInvalidCodeThrows() {
		LobbyService lobbyService = new LobbyService();

		assertThatThrownBy(() -> lobbyService.returnToLobby("XXXXXX", new Client("Alice")))
			.isInstanceOf(GameException.class)
			.hasFieldOrPropertyWithValue("errorCode", "LOBBY_NOT_FOUND");
	}

	@Test
	void startGameWithInvalidCodeThrows() {
		LobbyService lobbyService = new LobbyService();

		assertThatThrownBy(() -> lobbyService.startGame("XXXXXX", new Client("Alice")))
			.isInstanceOf(GameException.class)
			.hasFieldOrPropertyWithValue("errorCode", "LOBBY_NOT_FOUND");
	}
}
