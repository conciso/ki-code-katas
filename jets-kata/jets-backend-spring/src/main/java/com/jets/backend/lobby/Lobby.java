package com.jets.backend.lobby;

import java.util.ArrayList;

import com.jets.backend.GameException;
import com.jets.backend.model.Client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Lobby {

    private String code;
    private ArrayList<Client> players;
    private boolean inProgress;
    private String gameMode = "COOP";
    
	public Lobby(Client player) {
		players = new ArrayList<Client>();
		players.add(player);
	}

    public void setCode(String code) {
        if (code == null || !code.matches("[A-Z0-9]{6}")) throw new IllegalArgumentException();
        this.code = code;
    }

    public int getNumberPlayers() {
        if (players == null) return 0;
        return players.size();
    }

    public void addPlayer(Client client) {
        if (players == null) players = new ArrayList<>();
        if (players.size() >= 4) throw new GameException("LOBBY_FULL", "Die Lobby ist voll (max. 4 Spieler)");
        players.add(client);
    }

    public Client getPlayer(int number) {
    	if(number < 0 || players == null || number >= players.size()) {
    		throw new IllegalArgumentException();
    	}
    	return players.get(number);
    }
    
    public Client getHost() {
    	return getPlayer(0);
    }

}
