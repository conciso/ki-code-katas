using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using Microsoft.AspNetCore.Mvc.Testing;
using jets_backend_dotnet;

namespace jets_backend_dotnet.Tests;

public class GameManagerTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly WebApplicationFactory<Program> _factory;

    public GameManagerTests(WebApplicationFactory<Program> factory)
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

    private static async Task SendAsync(WebSocket ws, string message)
    {
        var bytes = Encoding.UTF8.GetBytes(message);
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

    // --- Player-ID-Generierung ---

    [Fact]
    public async Task GeneratedPlayerId_StartsWithP()
    {
        using var ws = await ConnectAsync("Alice");
        var data = ParseData(await ReceiveAsync(ws));

        var playerId = data.GetProperty("playerId").GetString()!;
        Assert.StartsWith("p", playerId);
    }

    [Fact]
    public async Task GeneratedPlayerId_HasCorrectLength()
    {
        using var ws = await ConnectAsync("Alice");
        var data = ParseData(await ReceiveAsync(ws));

        var playerId = data.GetProperty("playerId").GetString()!;
        Assert.Equal(8, playerId.Length); // "p" + 7 hex chars
    }

    // --- Spielerverwaltung ---

    [Fact]
    public async Task HandleConnection_TracksPlayer()
    {
        using var ws1 = await ConnectAsync("Alice");
        using var ws2 = await ConnectAsync("Bob");
        await ReceiveAsync(ws1);
        await ReceiveAsync(ws2);

        // Beide verbunden — wenn Bob disconnectet, bekommt Alice eine Nachricht
        ws2.Abort();
        var notification = await ReceiveAsync(ws1);

        Assert.Equal("DISCONNECTED", GetType(notification));
    }

    [Fact]
    public async Task HandleConnection_RemovesPlayerAfterDisconnect()
    {
        using var ws1 = await ConnectAsync("Alice");
        using var ws2 = await ConnectAsync("Bob");
        using var ws3 = await ConnectAsync("Charlie");
        await ReceiveAsync(ws1);
        await ReceiveAsync(ws2);
        await ReceiveAsync(ws3);

        // Bob disconnectet
        ws2.Abort();

        // Alice und Charlie bekommen DISCONNECTED
        var notif1 = await ReceiveAsync(ws1);
        var notif3 = await ReceiveAsync(ws3);
        Assert.Equal("DISCONNECTED", GetType(notif1));
        Assert.Equal("DISCONNECTED", GetType(notif3));

        // Charlie disconnectet — nur Alice bekommt Nachricht (nicht Bob)
        ws3.Abort();
        var notif = await ReceiveAsync(ws1);
        Assert.Equal("DISCONNECTED", GetType(notif));
        Assert.Equal("Charlie", ParseData(notif).GetProperty("playerName").GetString());
    }

    // --- Nachrichtenrouting ---

    [Fact]
    public async Task HandleMessage_PingReturnsPong()
    {
        using var ws = await ConnectAsync();
        await ReceiveAsync(ws); // consume CONNECTED

        var ping = JsonSerializer.Serialize(new { type = "PING", data = new { timestamp = 42L } });
        await SendAsync(ws, ping);
        var response = await ReceiveAsync(ws);

        Assert.Equal("PONG", GetType(response));
        Assert.Equal(42L, ParseData(response).GetProperty("timestamp").GetInt64());
    }

    [Fact]
    public async Task HandleMessage_InvalidJsonReturnsError()
    {
        using var ws = await ConnectAsync();
        await ReceiveAsync(ws);

        await SendAsync(ws, "broken{json");
        var response = await ReceiveAsync(ws);

        Assert.Equal("ERROR", GetType(response));
        Assert.Equal("INVALID_MESSAGE", ParseData(response).GetProperty("code").GetString());
    }

    [Fact]
    public async Task HandleMessage_UnknownTypeReturnsError()
    {
        using var ws = await ConnectAsync();
        await ReceiveAsync(ws);

        await SendAsync(ws, JsonSerializer.Serialize(new { type = "FOOBAR", data = new { } }));
        var response = await ReceiveAsync(ws);

        Assert.Equal("ERROR", GetType(response));
        Assert.Equal("INVALID_MESSAGE", ParseData(response).GetProperty("code").GetString());
    }

    [Fact]
    public async Task HandleMessage_MissingTypeFieldReturnsError()
    {
        using var ws = await ConnectAsync();
        await ReceiveAsync(ws);

        await SendAsync(ws, JsonSerializer.Serialize(new { data = new { } }));
        var response = await ReceiveAsync(ws);

        Assert.Equal("ERROR", GetType(response));
        Assert.Equal("INVALID_MESSAGE", ParseData(response).GetProperty("code").GetString());
    }

    [Fact]
    public async Task HandleMessage_PingWithoutTimestampReturnsError()
    {
        using var ws = await ConnectAsync();
        await ReceiveAsync(ws);

        await SendAsync(ws, JsonSerializer.Serialize(new { type = "PING", data = new { } }));
        var response = await ReceiveAsync(ws);

        Assert.Equal("ERROR", GetType(response));
        Assert.Equal("INVALID_MESSAGE", ParseData(response).GetProperty("code").GetString());
    }
}
