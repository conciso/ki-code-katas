using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using Microsoft.AspNetCore.Mvc.Testing;

namespace jets_backend_dotnet.Tests;

public class WebSocketTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly WebApplicationFactory<Program> _factory;

    public WebSocketTests(WebApplicationFactory<Program> factory)
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

    private static async Task<string> SendAndReceiveAsync(WebSocket ws, string message)
    {
        await SendAsync(ws, message);
        return await ReceiveAsync(ws);
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

    // --- CONNECTED ---

    [Fact]
    public async Task Connect_ReceivesConnectedMessage()
    {
        using var ws = await ConnectAsync("Alice");

        var response = await ReceiveAsync(ws);

        Assert.Equal("CONNECTED", GetType(response));
    }

    [Fact]
    public async Task Connect_ConnectedMessageContainsPlayerId()
    {
        using var ws = await ConnectAsync("Alice");

        var response = await ReceiveAsync(ws);
        var data = ParseData(response);

        Assert.False(string.IsNullOrEmpty(data.GetProperty("playerId").GetString()));
    }

    [Fact]
    public async Task Connect_ConnectedMessageContainsServerTickRate()
    {
        using var ws = await ConnectAsync("Alice");

        var response = await ReceiveAsync(ws);
        var data = ParseData(response);

        Assert.Equal(30, data.GetProperty("serverTickRate").GetInt32());
    }

    [Fact]
    public async Task Connect_DifferentPlayersGetDifferentIds()
    {
        using var ws1 = await ConnectAsync("Alice");
        using var ws2 = await ConnectAsync("Bob");

        var data1 = ParseData(await ReceiveAsync(ws1));
        var data2 = ParseData(await ReceiveAsync(ws2));

        Assert.NotEqual(
            data1.GetProperty("playerId").GetString(),
            data2.GetProperty("playerId").GetString());
    }

    // --- PING / PONG ---

    [Fact]
    public async Task Ping_ReturnsPongWithTimestamp()
    {
        using var ws = await ConnectAsync();
        await ReceiveAsync(ws); // consume CONNECTED

        var ping = JsonSerializer.Serialize(new { type = "PING", data = new { timestamp = 1234567890L } });
        var response = await SendAndReceiveAsync(ws, ping);

        Assert.Equal("PONG", GetType(response));
        var data = ParseData(response);
        Assert.Equal(1234567890L, data.GetProperty("timestamp").GetInt64());
    }

    [Fact]
    public async Task Ping_MeasuresLatency()
    {
        using var ws = await ConnectAsync();
        await ReceiveAsync(ws); // consume CONNECTED

        var timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        var ping = JsonSerializer.Serialize(new { type = "PING", data = new { timestamp } });
        var response = await SendAndReceiveAsync(ws, ping);

        var data = ParseData(response);
        var returnedTimestamp = data.GetProperty("timestamp").GetInt64();
        var latency = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() - returnedTimestamp;

        Assert.Equal(timestamp, returnedTimestamp);
        Assert.True(latency >= 0, "Latency should be non-negative");
    }

    // --- DISCONNECTED ---

    [Fact]
    public async Task Disconnect_OtherClientsReceiveDisconnectedMessage()
    {
        using var ws1 = await ConnectAsync("Alice");
        using var ws2 = await ConnectAsync("Bob");
        await ReceiveAsync(ws1); // consume CONNECTED
        var data2 = ParseData(await ReceiveAsync(ws2));
        var bobId = data2.GetProperty("playerId").GetString();

        ws2.Abort();

        var notification = await ReceiveAsync(ws1);
        Assert.Equal("DISCONNECTED", GetType(notification));
        var disconnectData = ParseData(notification);
        Assert.Equal(bobId, disconnectData.GetProperty("playerId").GetString());
        Assert.Equal("Bob", disconnectData.GetProperty("playerName").GetString());
    }

    // --- ERROR ---

    [Fact]
    public async Task InvalidMessage_ReturnsError()
    {
        using var ws = await ConnectAsync();
        await ReceiveAsync(ws); // consume CONNECTED

        var response = await SendAndReceiveAsync(ws, "not valid json!!!");

        Assert.Equal("ERROR", GetType(response));
        var data = ParseData(response);
        Assert.Equal("INVALID_MESSAGE", data.GetProperty("code").GetString());
    }

    [Fact]
    public async Task UnknownMessageType_ReturnsError()
    {
        using var ws = await ConnectAsync();
        await ReceiveAsync(ws); // consume CONNECTED

        var message = JsonSerializer.Serialize(new { type = "UNKNOWN_TYPE", data = new { } });
        var response = await SendAndReceiveAsync(ws, message);

        Assert.Equal("ERROR", GetType(response));
        var data = ParseData(response);
        Assert.Equal("INVALID_MESSAGE", data.GetProperty("code").GetString());
    }
}
