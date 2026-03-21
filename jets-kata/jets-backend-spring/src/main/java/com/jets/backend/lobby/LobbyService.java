package com.jets.backend.lobby;

import java.util.ArrayList;
import java.util.Random;

import com.jets.backend.GameException;
import com.jets.backend.model.Client;

public class LobbyService {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final Random random = new Random();

    private ArrayList<Lobby> lobbies = new ArrayList<>();
    
    public Lobby createLobby(Client player) {
        Lobby lobby = new Lobby(player);
        lobby.setCode(generateCode());
        lobbies.add(lobby);
        return lobby;
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return code.toString();
    }

	public void joinLobby(String code, Client player) {
		for (Lobby l : lobbies) {
			if (!l.getCode().equals(code)) continue;
			if (l.isInProgress()) throw new GameException("GAME_IN_PROGRESS", "Spiel hat bereits begonnen");
			l.addPlayer(player);
			return;
		}
		throw new GameException("LOBBY_NOT_FOUND", "Lobby-Code existiert nicht");
	}

	public void setReady(String code, Client player, boolean ready) {
		findLobby(code);
		player.setReady(ready);
	}

	public void startGame(String code, Client requester) {
		Lobby lobby = findLobby(code);
		if (!lobby.getHost().equals(requester))
			throw new GameException("NOT_HOST", "Nur der Host darf diese Aktion ausführen");
		if (lobby.getNumberPlayers() < 2)
			throw new GameException("NOT_ENOUGH_PLAYERS", "Mindestens 2 Spieler erforderlich");
		if (lobby.getPlayers().stream().anyMatch(p -> !p.isReady()))
			throw new GameException("NOT_ALL_READY", "Nicht alle Spieler sind bereit");
		lobby.setInProgress(true);
	}

	public void leaveLobby(String code, Client player) {
		Lobby lobby = findLobby(code);
		lobby.getPlayers().remove(player);
	}

	public void setGameMode(String code, Client requester, String gameMode) {
		Lobby lobby = findLobby(code);
		if (!lobby.getHost().equals(requester))
			throw new GameException("NOT_HOST", "Nur der Host darf diese Aktion ausführen");
		lobby.setGameMode(gameMode);
	}

	public void returnToLobby(String code, Client requester) {
		Lobby lobby = findLobby(code);
		if (!lobby.getHost().equals(requester))
			throw new GameException("NOT_HOST", "Nur der Host darf diese Aktion ausführen");
		lobby.setInProgress(false);
		lobby.getPlayers().forEach(p -> p.setReady(false));
	}

	public Lobby getLobby(String code) {
		return findLobby(code);
	}

	private Lobby findLobby(String code) {
		return lobbies.stream()
			.filter(l -> l.getCode().equals(code))
			.findFirst()
			.orElseThrow(() -> new GameException("LOBBY_NOT_FOUND", "Lobby-Code existiert nicht"));
	}
}
