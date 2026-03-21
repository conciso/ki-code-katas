using jets_backend_dotnet;

namespace jets_backend_dotnet.Tests;

public class LobbyTests
{
    private static Player CreatePlayer(string id = "p1234567", string name = "Alice") =>
        new(id, name, new System.Net.WebSockets.ClientWebSocket());

    // --- Lobby-Erstellung ---

    [Fact]
    public void Lobby_HasSixCharCode()
    {
        var lobby = new Lobby(CreatePlayer());

        Assert.Equal(6, lobby.Code.Length);
    }

    [Fact]
    public void Lobby_CodeIsUppercaseAlphanumeric()
    {
        var lobby = new Lobby(CreatePlayer());

        Assert.Matches("^[A-Z0-9]{6}$", lobby.Code);
    }

    [Fact]
    public void Lobby_HostIsFirstPlayer()
    {
        var player = CreatePlayer("p1111111", "Alice");
        var lobby = new Lobby(player);

        Assert.Equal("p1111111", lobby.HostId);
    }

    [Fact]
    public void Lobby_DefaultGameModeIsFFA()
    {
        var lobby = new Lobby(CreatePlayer());

        Assert.Equal("FFA", lobby.GameMode);
    }

    [Fact]
    public void Lobby_StartsWithOnePlayer()
    {
        var lobby = new Lobby(CreatePlayer());

        Assert.Single(lobby.Players);
    }

    // --- Spieler hinzufügen ---

    [Fact]
    public void AddPlayer_IncreasesPlayerCount()
    {
        var lobby = new Lobby(CreatePlayer("p1111111", "Alice"));

        lobby.AddPlayer(CreatePlayer("p2222222", "Bob"));

        Assert.Equal(2, lobby.Players.Count);
    }

    [Fact]
    public void AddPlayer_FourthPlayerIsAllowed()
    {
        var lobby = new Lobby(CreatePlayer("p1111111", "Alice"));
        lobby.AddPlayer(CreatePlayer("p2222222", "Bob"));
        lobby.AddPlayer(CreatePlayer("p3333333", "Charlie"));

        lobby.AddPlayer(CreatePlayer("p4444444", "Dave"));

        Assert.Equal(4, lobby.Players.Count);
    }

    [Fact]
    public void IsFull_FalseWithLessThanFourPlayers()
    {
        var lobby = new Lobby(CreatePlayer());

        Assert.False(lobby.IsFull);
    }

    [Fact]
    public void IsFull_TrueWithFourPlayers()
    {
        var lobby = new Lobby(CreatePlayer("p1111111", "Alice"));
        lobby.AddPlayer(CreatePlayer("p2222222", "Bob"));
        lobby.AddPlayer(CreatePlayer("p3333333", "Charlie"));
        lobby.AddPlayer(CreatePlayer("p4444444", "Dave"));

        Assert.True(lobby.IsFull);
    }

    // --- Spieler entfernen ---

    [Fact]
    public void RemovePlayer_DecreasesPlayerCount()
    {
        var lobby = new Lobby(CreatePlayer("p1111111", "Alice"));
        lobby.AddPlayer(CreatePlayer("p2222222", "Bob"));

        lobby.RemovePlayer("p2222222");

        Assert.Single(lobby.Players);
    }

    [Fact]
    public void RemoveHost_TransfersHostToNextPlayer()
    {
        var lobby = new Lobby(CreatePlayer("p1111111", "Alice"));
        lobby.AddPlayer(CreatePlayer("p2222222", "Bob"));

        lobby.RemovePlayer("p1111111");

        Assert.Equal("p2222222", lobby.HostId);
    }

    // --- Spieler-Eigenschaften ---

    [Fact]
    public void Players_HaveReadyDefaultFalse()
    {
        var lobby = new Lobby(CreatePlayer());

        Assert.False(lobby.Players[0].Ready);
    }

    [Fact]
    public void Players_HaveColor()
    {
        var lobby = new Lobby(CreatePlayer());

        Assert.False(string.IsNullOrEmpty(lobby.Players[0].Color));
    }

    [Fact]
    public void Players_GetDifferentColors()
    {
        var lobby = new Lobby(CreatePlayer("p1111111", "Alice"));
        lobby.AddPlayer(CreatePlayer("p2222222", "Bob"));

        Assert.NotEqual(lobby.Players[0].Color, lobby.Players[1].Color);
    }
}
