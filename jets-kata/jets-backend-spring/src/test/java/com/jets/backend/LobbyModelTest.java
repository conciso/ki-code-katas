package com.jets.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.jets.backend.lobby.Lobby;
import com.jets.backend.model.Client;

class LobbyModelTest {

	@Test
	void newLobbyHasOnePlayer() {
		Lobby lobby = new Lobby(new Client("Alice"));

		assertThat(lobby.getNumberPlayers()).isEqualTo(1);
	}

	@Test
	void newLobbyDefaultGameModeIsCoop() {
		Lobby lobby = new Lobby(new Client("Alice"));

		assertThat(lobby.getGameMode()).isEqualTo("COOP");
	}

	@Test
	void newLobbyIsNotInProgress() {
		Lobby lobby = new Lobby(new Client("Alice"));

		assertThat(lobby.isInProgress()).isFalse();
	}

	@Test
	void setCodeWithNullThrows() {
		Lobby lobby = new Lobby(new Client("Alice"));

		assertThatThrownBy(() -> lobby.setCode(null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void setCodeWithLowercaseThrows() {
		Lobby lobby = new Lobby(new Client("Alice"));

		assertThatThrownBy(() -> lobby.setCode("abcdef"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void setCodeWithTooShortCodeThrows() {
		Lobby lobby = new Lobby(new Client("Alice"));

		assertThatThrownBy(() -> lobby.setCode("ABC"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void setCodeWithTooLongCodeThrows() {
		Lobby lobby = new Lobby(new Client("Alice"));

		assertThatThrownBy(() -> lobby.setCode("ABCDEFG"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void setCodeWithSpecialCharsThrows() {
		Lobby lobby = new Lobby(new Client("Alice"));

		assertThatThrownBy(() -> lobby.setCode("ABC!@#"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void validCodeIsAccepted() {
		Lobby lobby = new Lobby(new Client("Alice"));

		lobby.setCode("ABC123");

		assertThat(lobby.getCode()).isEqualTo("ABC123");
	}

	@Test
	void getPlayerWithNegativeIndexThrows() {
		Lobby lobby = new Lobby(new Client("Alice"));

		assertThatThrownBy(() -> lobby.getPlayer(-1))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void getPlayerWithOutOfBoundsIndexThrows() {
		Lobby lobby = new Lobby(new Client("Alice"));

		assertThatThrownBy(() -> lobby.getPlayer(1))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void addPlayerWhenFullThrowsLobbyFull() {
		Lobby lobby = new Lobby(new Client("P1"));
		lobby.addPlayer(new Client("P2"));
		lobby.addPlayer(new Client("P3"));
		lobby.addPlayer(new Client("P4"));

		assertThatThrownBy(() -> lobby.addPlayer(new Client("P5")))
			.isInstanceOf(GameException.class)
			.hasFieldOrPropertyWithValue("errorCode", "LOBBY_FULL");
	}

	// --- Defensive null-checks via @Builder (players=null Zustand) ---

	@Test
	void getNumberPlayersWithNullPlayersReturnsZero() {
		Lobby lobby = Lobby.builder().build(); // players=null via Builder

		assertThat(lobby.getNumberPlayers()).isEqualTo(0);
	}

	@Test
	void addPlayerWithNullPlayersInitializesAndAdds() {
		Lobby lobby = Lobby.builder().build(); // players=null via Builder

		lobby.addPlayer(new Client("Alice"));

		assertThat(lobby.getNumberPlayers()).isEqualTo(1);
	}

	@Test
	void getPlayerWithNullPlayersThrows() {
		Lobby lobby = Lobby.builder().build(); // players=null via Builder

		assertThatThrownBy(() -> lobby.getPlayer(0))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
