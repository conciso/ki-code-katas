using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using Microsoft.AspNetCore.Mvc.Testing;

namespace jets_backend_dotnet.Tests;

public class LobbyFlowTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly WebApplicationFactory<Program> _factory;

    public LobbyFlowTests(WebApplicationFactory<Program> factory)
    {
        _factory = factory;
    }

    private async Task<WebSocket> ConnectAsync(string playerName = "TestPlayer")
    {
        var client = _factory.Server.CreateWebSocketClient();
        return await client.ConnectAsync(
            new Uri($"ws://localhost/ws/game?playerName={playerName}"), CancellationToken.None);
    }

    private static async Task<string> ReceiveAsync(WebSocket ws, int timeoutMs = 2000)
    {
        using var cts = new CancellationTokenSource(timeoutMs);
        var buffer = new byte[4096];
        var result = await ws.ReceiveAsync(buffer, cts.Token);
        return Encoding.UTF8.GetString(buffer, 0, result.Count);
    }

    private static async Task SendAsync(WebSocket ws, object message)
    {
        var json = JsonSerializer.Serialize(message);
        var bytes = Encoding.UTF8.GetBytes(json);
        await ws.SendAsync(bytes, WebSocketMessageType.Text, true, CancellationToken.None);
    }

    private static JsonElement ParseData(string json)
    {
        using var doc = JsonDocument.Parse(json);
        return doc.RootElement.GetProperty("data").Clone();
    }

    private static string GetType(string json)
    {
        using var doc = JsonDocument.Parse(json);
        return doc.RootElement.GetProperty("type").GetString()!;
    }

    private async Task<string> CreateLobbyAndGetCode(WebSocket ws)
    {
        await SendAsync(ws, new { type = "CREATE_LOBBY", data = new { playerName = "Host" } });
        var data = ParseData(await ReceiveAsync(ws)); // LOBBY_CREATED
        await ReceiveAsync(ws); // consume LOBBY_STATE
        return data.GetProperty("lobbyCode").GetString()!;
    }

    // --- CREATE_LOBBY ---

    [Fact]
    public async Task CreateLobby_ReturnsLobbyCreated()
    {
        using var ws = await ConnectAsync("Alice");
        await ReceiveAsync(ws); // consume CONNECTED

        await SendAsync(ws, new { type = "CREATE_LOBBY", data = new { playerName = "Alice" } });
        var response = await ReceiveAsync(ws);

        Assert.Equal("LOBBY_CREATED", GetType(response));
    }

    [Fact]
    public async Task CreateLobby_ContainsLobbyCode()
    {
        using var ws = await ConnectAsync("Alice");
        await ReceiveAsync(ws);

        await SendAsync(ws, new { type = "CREATE_LOBBY", data = new { playerName = "Alice" } });
        var data = ParseData(await ReceiveAsync(ws));

        var lobbyCode = data.GetProperty("lobbyCode").GetString()!;
        Assert.Equal(6, lobbyCode.Length);
        Assert.Matches("^[A-Z0-9]{6}$", lobbyCode);
    }

    [Fact]
    public async Task CreateLobby_ContainsHostId()
    {
        using var ws = await ConnectAsync("Alice");
        var connectedData = ParseData(await ReceiveAsync(ws));
        var playerId = connectedData.GetProperty("playerId").GetString();

        await SendAsync(ws, new { type = "CREATE_LOBBY", data = new { playerName = "Alice" } });
        var data = ParseData(await ReceiveAsync(ws));

        Assert.Equal(playerId, data.GetProperty("hostId").GetString());
    }

    [Fact]
    public async Task CreateLobby_SendsLobbyStateAfterCreated()
    {
        using var ws = await ConnectAsync("Alice");
        await ReceiveAsync(ws);

        await SendAsync(ws, new { type = "CREATE_LOBBY", data = new { playerName = "Alice" } });
        await ReceiveAsync(ws); // consume LOBBY_CREATED
        var lobbyState = await ReceiveAsync(ws);

        Assert.Equal("LOBBY_STATE", GetType(lobbyState));
    }

    [Fact]
    public async Task CreateLobby_LobbyStateContainsOnePlayer()
    {
        using var ws = await ConnectAsync("Alice");
        await ReceiveAsync(ws);

        await SendAsync(ws, new { type = "CREATE_LOBBY", data = new { playerName = "Alice" } });
        await ReceiveAsync(ws); // consume LOBBY_CREATED
        var data = ParseData(await ReceiveAsync(ws));

        var players = data.GetProperty("players");
        Assert.Equal(1, players.GetArrayLength());
        Assert.Equal("Alice", players[0].GetProperty("name").GetString());
    }

    [Fact]
    public async Task CreateLobby_LobbyStateDefaultGameModeIsFFA()
    {
        using var ws = await ConnectAsync("Alice");
        await ReceiveAsync(ws);

        await SendAsync(ws, new { type = "CREATE_LOBBY", data = new { playerName = "Alice" } });
        await ReceiveAsync(ws); // consume LOBBY_CREATED
        var data = ParseData(await ReceiveAsync(ws));

        Assert.Equal("FFA", data.GetProperty("gameMode").GetString());
    }

    [Fact]
    public async Task CreateLobby_LobbyStatePlayerHasReadyFalse()
    {
        using var ws = await ConnectAsync("Alice");
        await ReceiveAsync(ws);

        await SendAsync(ws, new { type = "CREATE_LOBBY", data = new { playerName = "Alice" } });
        await ReceiveAsync(ws); // consume LOBBY_CREATED
        var data = ParseData(await ReceiveAsync(ws));

        Assert.False(data.GetProperty("players")[0].GetProperty("ready").GetBoolean());
    }

    [Fact]
    public async Task CreateLobby_LobbyStatePlayerHasColor()
    {
        using var ws = await ConnectAsync("Alice");
        await ReceiveAsync(ws);

        await SendAsync(ws, new { type = "CREATE_LOBBY", data = new { playerName = "Alice" } });
        await ReceiveAsync(ws); // consume LOBBY_CREATED
        var data = ParseData(await ReceiveAsync(ws));

        var color = data.GetProperty("players")[0].GetProperty("color").GetString();
        Assert.False(string.IsNullOrEmpty(color));
    }

    // --- JOIN_LOBBY ---

    [Fact]
    public async Task JoinLobby_HostReceivesLobbyState()
    {
        using var wsHost = await ConnectAsync("Alice");
        await ReceiveAsync(wsHost);
        var lobbyCode = await CreateLobbyAndGetCode(wsHost);

        using var wsJoiner = await ConnectAsync("Bob");
        await ReceiveAsync(wsJoiner);
        await SendAsync(wsJoiner, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = "Bob" } });

        var hostUpdate = await ReceiveAsync(wsHost);
        Assert.Equal("LOBBY_STATE", GetType(hostUpdate));
    }

    [Fact]
    public async Task JoinLobby_JoinerReceivesLobbyState()
    {
        using var wsHost = await ConnectAsync("Alice");
        await ReceiveAsync(wsHost);
        var lobbyCode = await CreateLobbyAndGetCode(wsHost);

        using var wsJoiner = await ConnectAsync("Bob");
        await ReceiveAsync(wsJoiner);
        await SendAsync(wsJoiner, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = "Bob" } });

        var joinerUpdate = await ReceiveAsync(wsJoiner);
        Assert.Equal("LOBBY_STATE", GetType(joinerUpdate));
    }

    [Fact]
    public async Task JoinLobby_LobbyStateContainsTwoPlayers()
    {
        using var wsHost = await ConnectAsync("Alice");
        await ReceiveAsync(wsHost);
        var lobbyCode = await CreateLobbyAndGetCode(wsHost);

        using var wsJoiner = await ConnectAsync("Bob");
        await ReceiveAsync(wsJoiner);
        await SendAsync(wsJoiner, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = "Bob" } });

        var data = ParseData(await ReceiveAsync(wsJoiner));
        var players = data.GetProperty("players");
        Assert.Equal(2, players.GetArrayLength());
    }

    [Fact]
    public async Task JoinLobby_HostIdRemainsOriginalHost()
    {
        using var wsHost = await ConnectAsync("Alice");
        var connectedData = ParseData(await ReceiveAsync(wsHost));
        var hostId = connectedData.GetProperty("playerId").GetString();
        var lobbyCode = await CreateLobbyAndGetCode(wsHost);

        using var wsJoiner = await ConnectAsync("Bob");
        await ReceiveAsync(wsJoiner);
        await SendAsync(wsJoiner, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = "Bob" } });

        var data = ParseData(await ReceiveAsync(wsJoiner));
        Assert.Equal(hostId, data.GetProperty("hostId").GetString());
    }

    [Fact]
    public async Task JoinLobby_InvalidCode_ReturnsLobbyNotFound()
    {
        using var ws = await ConnectAsync("Bob");
        await ReceiveAsync(ws);

        await SendAsync(ws, new { type = "JOIN_LOBBY", data = new { lobbyCode = "ZZZZZZ", playerName = "Bob" } });
        var response = await ReceiveAsync(ws);

        Assert.Equal("ERROR", GetType(response));
        Assert.Equal("LOBBY_NOT_FOUND", ParseData(response).GetProperty("code").GetString());
    }

    [Fact]
    public async Task JoinLobby_FullLobby_ReturnsLobbyFull()
    {
        using var wsHost = await ConnectAsync("Alice");
        await ReceiveAsync(wsHost);
        var lobbyCode = await CreateLobbyAndGetCode(wsHost);

        // Fill lobby to 4 players
        for (var i = 0; i < 3; i++)
        {
            var ws = await ConnectAsync($"Player{i}");
            await ReceiveAsync(ws);
            await SendAsync(ws, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = $"Player{i}" } });
            await ReceiveAsync(ws); // consume LOBBY_STATE
            await ReceiveAsync(wsHost); // consume LOBBY_STATE on host
        }

        // 5th player tries to join
        using var wsFifth = await ConnectAsync("TooMany");
        await ReceiveAsync(wsFifth);
        await SendAsync(wsFifth, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = "TooMany" } });
        var response = await ReceiveAsync(wsFifth);

        Assert.Equal("ERROR", GetType(response));
        Assert.Equal("LOBBY_FULL", ParseData(response).GetProperty("code").GetString());
    }

    // --- LEAVE_LOBBY ---

    [Fact]
    public async Task LeaveLobby_RemainingPlayersReceiveLobbyState()
    {
        using var wsHost = await ConnectAsync("Alice");
        await ReceiveAsync(wsHost);
        var lobbyCode = await CreateLobbyAndGetCode(wsHost);

        using var wsJoiner = await ConnectAsync("Bob");
        await ReceiveAsync(wsJoiner);
        await SendAsync(wsJoiner, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = "Bob" } });
        await ReceiveAsync(wsJoiner); // consume LOBBY_STATE (join)
        await ReceiveAsync(wsHost);   // consume LOBBY_STATE (join)

        await SendAsync(wsJoiner, new { type = "LEAVE_LOBBY", data = new { } });
        var update = await ReceiveAsync(wsHost);

        Assert.Equal("LOBBY_STATE", GetType(update));
        Assert.Equal(1, ParseData(update).GetProperty("players").GetArrayLength());
    }

    [Fact]
    public async Task LeaveLobby_HostLeaves_TransfersHost()
    {
        using var wsHost = await ConnectAsync("Alice");
        var hostConnected = ParseData(await ReceiveAsync(wsHost));
        var lobbyCode = await CreateLobbyAndGetCode(wsHost);

        using var wsJoiner = await ConnectAsync("Bob");
        var joinerConnected = ParseData(await ReceiveAsync(wsJoiner));
        var bobId = joinerConnected.GetProperty("playerId").GetString();
        await SendAsync(wsJoiner, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = "Bob" } });
        await ReceiveAsync(wsJoiner); // consume LOBBY_STATE (join)
        await ReceiveAsync(wsHost);   // consume LOBBY_STATE (join)

        await SendAsync(wsHost, new { type = "LEAVE_LOBBY", data = new { } });
        var update = await ReceiveAsync(wsJoiner);

        Assert.Equal("LOBBY_STATE", GetType(update));
        Assert.Equal(bobId, ParseData(update).GetProperty("hostId").GetString());
    }

    // --- PLAYER_READY ---

    [Fact]
    public async Task PlayerReady_BroadcastsLobbyState()
    {
        using var wsHost = await ConnectAsync("Alice");
        await ReceiveAsync(wsHost);
        var lobbyCode = await CreateLobbyAndGetCode(wsHost);

        using var wsJoiner = await ConnectAsync("Bob");
        await ReceiveAsync(wsJoiner);
        await SendAsync(wsJoiner, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = "Bob" } });
        await ReceiveAsync(wsJoiner); // consume LOBBY_STATE
        await ReceiveAsync(wsHost);   // consume LOBBY_STATE

        await SendAsync(wsJoiner, new { type = "PLAYER_READY", data = new { ready = true } });
        var update = await ReceiveAsync(wsHost);

        Assert.Equal("LOBBY_STATE", GetType(update));
        var players = ParseData(update).GetProperty("players");
        // Bob is second player, should be ready
        Assert.True(players[1].GetProperty("ready").GetBoolean());
    }

    [Fact]
    public async Task PlayerReady_CanToggleOff()
    {
        using var wsHost = await ConnectAsync("Alice");
        await ReceiveAsync(wsHost);
        var lobbyCode = await CreateLobbyAndGetCode(wsHost);

        using var wsJoiner = await ConnectAsync("Bob");
        await ReceiveAsync(wsJoiner);
        await SendAsync(wsJoiner, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = "Bob" } });
        await ReceiveAsync(wsJoiner); // consume LOBBY_STATE
        await ReceiveAsync(wsHost);   // consume LOBBY_STATE

        await SendAsync(wsJoiner, new { type = "PLAYER_READY", data = new { ready = true } });
        await ReceiveAsync(wsHost);   // consume LOBBY_STATE
        await ReceiveAsync(wsJoiner); // consume LOBBY_STATE

        await SendAsync(wsJoiner, new { type = "PLAYER_READY", data = new { ready = false } });
        var update = await ReceiveAsync(wsJoiner);

        var players = ParseData(update).GetProperty("players");
        Assert.False(players[1].GetProperty("ready").GetBoolean());
    }

    // --- START_GAME ---

    [Fact]
    public async Task StartGame_NonHostGetsError()
    {
        using var wsHost = await ConnectAsync("Alice");
        await ReceiveAsync(wsHost);
        var lobbyCode = await CreateLobbyAndGetCode(wsHost);

        using var wsJoiner = await ConnectAsync("Bob");
        await ReceiveAsync(wsJoiner);
        await SendAsync(wsJoiner, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = "Bob" } });
        await ReceiveAsync(wsJoiner);
        await ReceiveAsync(wsHost);

        await SendAsync(wsJoiner, new { type = "START_GAME", data = new { } });
        var response = await ReceiveAsync(wsJoiner);

        Assert.Equal("ERROR", GetType(response));
        Assert.Equal("NOT_HOST", ParseData(response).GetProperty("code").GetString());
    }

    [Fact]
    public async Task StartGame_NotEnoughPlayers_GetsError()
    {
        using var wsHost = await ConnectAsync("Alice");
        await ReceiveAsync(wsHost);
        await CreateLobbyAndGetCode(wsHost);

        await SendAsync(wsHost, new { type = "START_GAME", data = new { } });
        var response = await ReceiveAsync(wsHost);

        Assert.Equal("ERROR", GetType(response));
    }

    [Fact]
    public async Task StartGame_NotAllReady_GetsError()
    {
        using var wsHost = await ConnectAsync("Alice");
        await ReceiveAsync(wsHost);
        var lobbyCode = await CreateLobbyAndGetCode(wsHost);

        using var wsJoiner = await ConnectAsync("Bob");
        await ReceiveAsync(wsJoiner);
        await SendAsync(wsJoiner, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = "Bob" } });
        await ReceiveAsync(wsJoiner);
        await ReceiveAsync(wsHost);

        // Only host is ready, Bob is not
        await SendAsync(wsHost, new { type = "PLAYER_READY", data = new { ready = true } });
        await ReceiveAsync(wsHost);
        await ReceiveAsync(wsJoiner);

        await SendAsync(wsHost, new { type = "START_GAME", data = new { } });
        var response = await ReceiveAsync(wsHost);

        Assert.Equal("ERROR", GetType(response));
    }

    [Fact]
    public async Task StartGame_AllReady_SendsGameStarting()
    {
        using var wsHost = await ConnectAsync("Alice");
        await ReceiveAsync(wsHost);
        var lobbyCode = await CreateLobbyAndGetCode(wsHost);

        using var wsJoiner = await ConnectAsync("Bob");
        await ReceiveAsync(wsJoiner);
        await SendAsync(wsJoiner, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = "Bob" } });
        await ReceiveAsync(wsJoiner);
        await ReceiveAsync(wsHost);

        // Both ready
        await SendAsync(wsHost, new { type = "PLAYER_READY", data = new { ready = true } });
        await ReceiveAsync(wsHost);
        await ReceiveAsync(wsJoiner);
        await SendAsync(wsJoiner, new { type = "PLAYER_READY", data = new { ready = true } });
        await ReceiveAsync(wsHost);
        await ReceiveAsync(wsJoiner);

        await SendAsync(wsHost, new { type = "START_GAME", data = new { } });
        var response = await ReceiveAsync(wsHost);

        Assert.Equal("GAME_STARTING", GetType(response));
        var data = ParseData(response);
        Assert.Equal(3, data.GetProperty("countdown").GetInt32());
        Assert.Equal("FFA", data.GetProperty("gameMode").GetString());
        var players = data.GetProperty("players");
        Assert.Equal(2, players.GetArrayLength());
        Assert.Equal("Alice", players[0].GetProperty("name").GetString());
        Assert.Equal("Bob", players[1].GetProperty("name").GetString());
        Assert.False(string.IsNullOrEmpty(players[0].GetProperty("color").GetString()));
        Assert.False(string.IsNullOrEmpty(players[0].GetProperty("id").GetString()));
    }

    [Fact]
    public async Task StartGame_AllReady_BothClientsReceiveGameStarting()
    {
        using var wsHost = await ConnectAsync("Alice");
        await ReceiveAsync(wsHost);
        var lobbyCode = await CreateLobbyAndGetCode(wsHost);

        using var wsJoiner = await ConnectAsync("Bob");
        await ReceiveAsync(wsJoiner);
        await SendAsync(wsJoiner, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = "Bob" } });
        await ReceiveAsync(wsJoiner);
        await ReceiveAsync(wsHost);

        await SendAsync(wsHost, new { type = "PLAYER_READY", data = new { ready = true } });
        await ReceiveAsync(wsHost);
        await ReceiveAsync(wsJoiner);
        await SendAsync(wsJoiner, new { type = "PLAYER_READY", data = new { ready = true } });
        await ReceiveAsync(wsHost);
        await ReceiveAsync(wsJoiner);

        await SendAsync(wsHost, new { type = "START_GAME", data = new { } });
        await ReceiveAsync(wsHost); // consume GAME_STARTING on host

        var joinerResponse = await ReceiveAsync(wsJoiner);
        Assert.Equal("GAME_STARTING", GetType(joinerResponse));
        var data = ParseData(joinerResponse);
        Assert.Equal(3, data.GetProperty("countdown").GetInt32());
        Assert.Equal(2, data.GetProperty("players").GetArrayLength());
    }

    // --- LOBBY_STATE vollständige Feld-Prüfung ---

    [Fact]
    public async Task JoinLobby_LobbyStateContainsAllFields()
    {
        using var wsHost = await ConnectAsync("Alice");
        var hostConnected = ParseData(await ReceiveAsync(wsHost));
        var hostId = hostConnected.GetProperty("playerId").GetString();
        var lobbyCode = await CreateLobbyAndGetCode(wsHost);

        using var wsJoiner = await ConnectAsync("Bob");
        await ReceiveAsync(wsJoiner);
        await SendAsync(wsJoiner, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = "Bob" } });

        var data = ParseData(await ReceiveAsync(wsJoiner));

        Assert.Equal(lobbyCode, data.GetProperty("lobbyCode").GetString());
        Assert.Equal(hostId, data.GetProperty("hostId").GetString());
        Assert.Equal("FFA", data.GetProperty("gameMode").GetString());

        var players = data.GetProperty("players");
        Assert.Equal(2, players.GetArrayLength());

        // Erster Spieler (Host)
        Assert.Equal(hostId, players[0].GetProperty("id").GetString());
        Assert.Equal("Alice", players[0].GetProperty("name").GetString());
        Assert.False(players[0].GetProperty("ready").GetBoolean());
        Assert.False(string.IsNullOrEmpty(players[0].GetProperty("color").GetString()));

        // Zweiter Spieler
        Assert.Equal("Bob", players[1].GetProperty("name").GetString());
        Assert.False(players[1].GetProperty("ready").GetBoolean());
        Assert.False(string.IsNullOrEmpty(players[1].GetProperty("color").GetString()));
        Assert.NotEqual(
            players[0].GetProperty("color").GetString(),
            players[1].GetProperty("color").GetString());
    }

    [Fact]
    public async Task LeaveLobby_LobbyStateContainsCorrectPlayerList()
    {
        using var wsHost = await ConnectAsync("Alice");
        await ReceiveAsync(wsHost);
        var lobbyCode = await CreateLobbyAndGetCode(wsHost);

        using var ws2 = await ConnectAsync("Bob");
        await ReceiveAsync(ws2);
        await SendAsync(ws2, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = "Bob" } });
        await ReceiveAsync(ws2);
        await ReceiveAsync(wsHost);

        using var ws3 = await ConnectAsync("Charlie");
        await ReceiveAsync(ws3);
        await SendAsync(ws3, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = "Charlie" } });
        await ReceiveAsync(ws3);
        await ReceiveAsync(wsHost);
        await ReceiveAsync(ws2);

        // Bob verlässt
        await SendAsync(ws2, new { type = "LEAVE_LOBBY", data = new { } });
        var update = await ReceiveAsync(wsHost);

        var data = ParseData(update);
        var players = data.GetProperty("players");
        Assert.Equal(2, players.GetArrayLength());
        Assert.Equal("Alice", players[0].GetProperty("name").GetString());
        Assert.Equal("Charlie", players[1].GetProperty("name").GetString());
    }
}
