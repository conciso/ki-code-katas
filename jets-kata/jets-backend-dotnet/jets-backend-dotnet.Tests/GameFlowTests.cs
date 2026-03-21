using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using Microsoft.AspNetCore.Mvc.Testing;

namespace jets_backend_dotnet.Tests;

public class GameFlowTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly WebApplicationFactory<Program> _factory;

    public GameFlowTests(WebApplicationFactory<Program> factory)
    {
        _factory = factory;
    }

    private async Task<WebSocket> ConnectAsync(string playerName = "TestPlayer")
    {
        var client = _factory.Server.CreateWebSocketClient();
        return await client.ConnectAsync(
            new Uri($"ws://localhost/ws/game?playerName={playerName}"), CancellationToken.None);
    }

    private static async Task<string> ReceiveAsync(WebSocket ws, int timeoutMs = 3000)
    {
        using var cts = new CancellationTokenSource(timeoutMs);
        var buffer = new byte[8192];
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

    private async Task<(WebSocket host, WebSocket joiner, string lobbyCode)> CreateReadyGame()
    {
        var wsHost = await ConnectAsync("Alice");
        await ReceiveAsync(wsHost); // CONNECTED

        await SendAsync(wsHost, new { type = "CREATE_LOBBY", data = new { playerName = "Alice" } });
        var lobbyCreated = ParseData(await ReceiveAsync(wsHost)); // LOBBY_CREATED
        var lobbyCode = lobbyCreated.GetProperty("lobbyCode").GetString()!;
        await ReceiveAsync(wsHost); // LOBBY_STATE

        var wsJoiner = await ConnectAsync("Bob");
        await ReceiveAsync(wsJoiner); // CONNECTED

        await SendAsync(wsJoiner, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = "Bob" } });
        await ReceiveAsync(wsJoiner); // LOBBY_STATE
        await ReceiveAsync(wsHost);   // LOBBY_STATE

        // Both ready
        await SendAsync(wsHost, new { type = "PLAYER_READY", data = new { ready = true } });
        await ReceiveAsync(wsHost);   // LOBBY_STATE
        await ReceiveAsync(wsJoiner); // LOBBY_STATE
        await SendAsync(wsJoiner, new { type = "PLAYER_READY", data = new { ready = true } });
        await ReceiveAsync(wsHost);   // LOBBY_STATE
        await ReceiveAsync(wsJoiner); // LOBBY_STATE

        return (wsHost, wsJoiner, lobbyCode);
    }

    private async Task StartGame(WebSocket wsHost, WebSocket wsJoiner)
    {
        await SendAsync(wsHost, new { type = "START_GAME", data = new { } });
        await ReceiveAsync(wsHost);   // GAME_STARTING
        await ReceiveAsync(wsJoiner); // GAME_STARTING
    }

    // --- GAME_STATE empfangen ---

    [Fact]
    public async Task GameStarted_ClientsReceiveGameState()
    {
        var (wsHost, wsJoiner, _) = await CreateReadyGame();
        await StartGame(wsHost, wsJoiner);

        var response = await ReceiveAsync(wsHost);

        Assert.Equal("GAME_STATE", GetType(response));
    }

    [Fact]
    public async Task GameState_ContainsTick()
    {
        var (wsHost, wsJoiner, _) = await CreateReadyGame();
        await StartGame(wsHost, wsJoiner);

        var data = ParseData(await ReceiveAsync(wsHost));

        Assert.True(data.GetProperty("tick").GetInt32() > 0);
    }

    [Fact]
    public async Task GameState_ContainsPlayers()
    {
        var (wsHost, wsJoiner, _) = await CreateReadyGame();
        await StartGame(wsHost, wsJoiner);

        var data = ParseData(await ReceiveAsync(wsHost));
        var players = data.GetProperty("players");

        Assert.Equal(2, players.GetArrayLength());
    }

    [Fact]
    public async Task GameState_PlayerHasAllFields()
    {
        var (wsHost, wsJoiner, _) = await CreateReadyGame();
        await StartGame(wsHost, wsJoiner);

        var data = ParseData(await ReceiveAsync(wsHost));
        var player = data.GetProperty("players")[0];

        Assert.False(string.IsNullOrEmpty(player.GetProperty("id").GetString()));
        Assert.True(player.TryGetProperty("x", out _));
        Assert.True(player.TryGetProperty("y", out _));
        Assert.Equal(3, player.GetProperty("hp").GetInt32());
        Assert.Equal(0, player.GetProperty("score").GetInt32());
        Assert.True(player.GetProperty("alive").GetBoolean());
        Assert.True(player.TryGetProperty("lastProcessedInput", out _));
    }

    [Fact]
    public async Task GameState_ContainsProjectilesArray()
    {
        var (wsHost, wsJoiner, _) = await CreateReadyGame();
        await StartGame(wsHost, wsJoiner);

        var data = ParseData(await ReceiveAsync(wsHost));

        Assert.True(data.TryGetProperty("projectiles", out var proj));
        Assert.Equal(JsonValueKind.Array, proj.ValueKind);
    }

    // --- PLAYER_INPUT ---

    [Fact]
    public async Task PlayerInput_AffectsGameState()
    {
        var (wsHost, wsJoiner, _) = await CreateReadyGame();
        await StartGame(wsHost, wsJoiner);

        // Ersten GAME_STATE lesen um Startposition zu bekommen
        var firstState = ParseData(await ReceiveAsync(wsHost));
        var startX = firstState.GetProperty("players")[0].GetProperty("x").GetDouble();

        // Input senden: nach rechts drehen
        await SendAsync(wsHost, new { type = "PLAYER_INPUT", data = new { up = false, down = false, left = false, right = true, shoot = false, seq = 1 } });

        // Ein paar GAME_STATEs lesen, Spieler sollte sich bewegt haben
        string? lastState = null;
        for (var i = 0; i < 10; i++)
            lastState = await ReceiveAsync(wsHost);

        var data = ParseData(lastState!);
        var currentX = data.GetProperty("players")[0].GetProperty("x").GetDouble();

        // Position sollte sich geändert haben (Spieler bewegt sich immer vorwärts)
        Assert.NotEqual(startX, currentX);
    }

    // --- GAME_IN_PROGRESS ---

    [Fact]
    public async Task JoinLobby_DuringGame_ReturnsError()
    {
        var (wsHost, wsJoiner, lobbyCode) = await CreateReadyGame();
        await StartGame(wsHost, wsJoiner);
        await ReceiveAsync(wsHost); // consume first GAME_STATE

        using var ws3 = await ConnectAsync("Charlie");
        await ReceiveAsync(ws3); // CONNECTED

        await SendAsync(ws3, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = "Charlie" } });
        var response = await ReceiveAsync(ws3);

        Assert.Equal("ERROR", GetType(response));
        Assert.Equal("GAME_IN_PROGRESS", ParseData(response).GetProperty("code").GetString());
    }
}
