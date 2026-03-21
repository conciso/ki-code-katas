using System.Net;
using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using Microsoft.AspNetCore.Mvc.Testing;

namespace jets_backend_dotnet.Tests;

public class DashboardTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly WebApplicationFactory<Program> _factory;

    public DashboardTests(WebApplicationFactory<Program> factory)
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

    [Fact]
    public async Task ApiLobbies_ReturnsOk()
    {
        var client = _factory.CreateClient();

        var response = await client.GetAsync("/api/lobbies");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task ApiLobbies_EmptyWhenNoLobbies()
    {
        var client = _factory.CreateClient();

        var response = await client.GetAsync("/api/lobbies");
        var json = await response.Content.ReadAsStringAsync();
        using var doc = JsonDocument.Parse(json);

        Assert.Equal(JsonValueKind.Array, doc.RootElement.ValueKind);
    }

    [Fact]
    public async Task ApiLobbies_ShowsCreatedLobby()
    {
        using var ws = await ConnectAsync("Alice");
        await ReceiveAsync(ws);
        await SendAsync(ws, new { type = "CREATE_LOBBY", data = new { playerName = "Alice" } });
        await ReceiveAsync(ws); // LOBBY_CREATED
        await ReceiveAsync(ws); // LOBBY_STATE

        var client = _factory.CreateClient();
        var response = await client.GetAsync("/api/lobbies");
        var json = await response.Content.ReadAsStringAsync();
        using var doc = JsonDocument.Parse(json);

        Assert.True(doc.RootElement.GetArrayLength() >= 1);
        var lobby = doc.RootElement[doc.RootElement.GetArrayLength() - 1];
        Assert.True(lobby.TryGetProperty("code", out _));
        Assert.True(lobby.TryGetProperty("playerCount", out _));
        Assert.True(lobby.TryGetProperty("status", out _));
        Assert.True(lobby.TryGetProperty("players", out _));
    }

    [Fact]
    public async Task ApiLobbies_ShowsCorrectPlayerCount()
    {
        using var ws1 = await ConnectAsync("Alice");
        await ReceiveAsync(ws1);
        await SendAsync(ws1, new { type = "CREATE_LOBBY", data = new { playerName = "Alice" } });
        var created = JsonDocument.Parse(await ReceiveAsync(ws1));
        var lobbyCode = created.RootElement.GetProperty("data").GetProperty("lobbyCode").GetString()!;
        await ReceiveAsync(ws1); // LOBBY_STATE

        using var ws2 = await ConnectAsync("Bob");
        await ReceiveAsync(ws2);
        await SendAsync(ws2, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = "Bob" } });
        await ReceiveAsync(ws2); // LOBBY_STATE
        await ReceiveAsync(ws1); // LOBBY_STATE

        var client = _factory.CreateClient();
        var response = await client.GetAsync("/api/lobbies");
        var json = await response.Content.ReadAsStringAsync();
        using var doc = JsonDocument.Parse(json);

        var lobby = doc.RootElement.EnumerateArray()
            .First(l => l.GetProperty("code").GetString() == lobbyCode);
        Assert.Equal(2, lobby.GetProperty("playerCount").GetInt32());
    }

    [Fact]
    public async Task ApiLobbies_ShowsInGameStatus()
    {
        using var ws1 = await ConnectAsync("Alice");
        await ReceiveAsync(ws1);
        await SendAsync(ws1, new { type = "CREATE_LOBBY", data = new { playerName = "Alice" } });
        var created = JsonDocument.Parse(await ReceiveAsync(ws1));
        var lobbyCode = created.RootElement.GetProperty("data").GetProperty("lobbyCode").GetString()!;
        await ReceiveAsync(ws1);

        using var ws2 = await ConnectAsync("Bob");
        await ReceiveAsync(ws2);
        await SendAsync(ws2, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = "Bob" } });
        await ReceiveAsync(ws2);
        await ReceiveAsync(ws1);

        await SendAsync(ws1, new { type = "PLAYER_READY", data = new { ready = true } });
        await ReceiveAsync(ws1);
        await ReceiveAsync(ws2);
        await SendAsync(ws2, new { type = "PLAYER_READY", data = new { ready = true } });
        await ReceiveAsync(ws1);
        await ReceiveAsync(ws2);

        await SendAsync(ws1, new { type = "START_GAME", data = new { } });
        await ReceiveAsync(ws1); // GAME_STARTING
        await ReceiveAsync(ws2); // GAME_STARTING

        var client = _factory.CreateClient();
        var response = await client.GetAsync("/api/lobbies");
        var json = await response.Content.ReadAsStringAsync();
        using var doc = JsonDocument.Parse(json);

        var lobby = doc.RootElement.EnumerateArray()
            .First(l => l.GetProperty("code").GetString() == lobbyCode);
        Assert.Equal("in_game", lobby.GetProperty("status").GetString());
    }

    [Fact]
    public async Task Dashboard_ReturnsHtml()
    {
        var client = _factory.CreateClient();

        var response = await client.GetAsync("/dashboard");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Equal("text/html", response.Content.Headers.ContentType?.MediaType);
    }

    // --- Disconnect Cleanup ---

    [Fact]
    public async Task Disconnect_RemovesPlayerFromLobby()
    {
        using var ws1 = await ConnectAsync("Alice");
        await ReceiveAsync(ws1);
        await SendAsync(ws1, new { type = "CREATE_LOBBY", data = new { playerName = "Alice" } });
        var created = JsonDocument.Parse(await ReceiveAsync(ws1));
        var lobbyCode = created.RootElement.GetProperty("data").GetProperty("lobbyCode").GetString()!;
        await ReceiveAsync(ws1); // LOBBY_STATE

        var ws2 = await ConnectAsync("Bob");
        await ReceiveAsync(ws2);
        await SendAsync(ws2, new { type = "JOIN_LOBBY", data = new { lobbyCode, playerName = "Bob" } });
        await ReceiveAsync(ws2); // LOBBY_STATE
        await ReceiveAsync(ws1); // LOBBY_STATE

        // Bob disconnectet
        ws2.Abort();
        await Task.Delay(200); // warten bis Disconnect verarbeitet

        var client = _factory.CreateClient();
        var response = await client.GetAsync("/api/lobbies");
        var json = await response.Content.ReadAsStringAsync();
        using var doc = JsonDocument.Parse(json);

        var lobby = doc.RootElement.EnumerateArray()
            .First(l => l.GetProperty("code").GetString() == lobbyCode);
        Assert.Equal(1, lobby.GetProperty("playerCount").GetInt32());
    }

    [Fact]
    public async Task Disconnect_LastPlayerRemovesLobby()
    {
        var ws = await ConnectAsync("Alice");
        await ReceiveAsync(ws);
        await SendAsync(ws, new { type = "CREATE_LOBBY", data = new { playerName = "Alice" } });
        var created = JsonDocument.Parse(await ReceiveAsync(ws));
        var lobbyCode = created.RootElement.GetProperty("data").GetProperty("lobbyCode").GetString()!;
        await ReceiveAsync(ws); // LOBBY_STATE

        // Alice disconnectet
        ws.Abort();
        await Task.Delay(200);

        var client = _factory.CreateClient();
        var response = await client.GetAsync("/api/lobbies");
        var json = await response.Content.ReadAsStringAsync();
        using var doc = JsonDocument.Parse(json);

        var lobbyExists = doc.RootElement.EnumerateArray()
            .Any(l => l.GetProperty("code").GetString() == lobbyCode);
        Assert.False(lobbyExists, "Lobby should be removed when last player disconnects");
    }
}
