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

    private async Task<WebSocket> ConnectAsync()
    {
        var client = _factory.Server.CreateWebSocketClient();
        return await client.ConnectAsync(new Uri("ws://localhost/ws/game"), CancellationToken.None);
    }

    private async Task<string> SendAndReceiveAsync(WebSocket ws, string message)
    {
        var sendBuffer = Encoding.UTF8.GetBytes(message);
        await ws.SendAsync(sendBuffer, WebSocketMessageType.Text, true, CancellationToken.None);

        var receiveBuffer = new byte[1024];
        var result = await ws.ReceiveAsync(receiveBuffer, CancellationToken.None);
        return Encoding.UTF8.GetString(receiveBuffer, 0, result.Count);
    }

    [Fact]
    public async Task WebSocket_ConnectsSuccessfully()
    {
        using var ws = await ConnectAsync();

        Assert.Equal(WebSocketState.Open, ws.State);
    }

    [Fact]
    public async Task WebSocket_EchoesMessage()
    {
        using var ws = await ConnectAsync();

        var received = await SendAndReceiveAsync(ws, "Hello, WebSocket!");

        Assert.Equal("Hello, WebSocket!", received);
    }

    [Fact]
    public async Task WebSocket_PingReturnsPongWithTimestamp()
    {
        using var ws = await ConnectAsync();

        var ping = JsonSerializer.Serialize(new { type = "ping", timestamp = 1234567890L });
        var response = await SendAndReceiveAsync(ws, ping);

        using var doc = JsonDocument.Parse(response);
        var root = doc.RootElement;
        Assert.Equal("pong", root.GetProperty("type").GetString());
        Assert.Equal(1234567890L, root.GetProperty("timestamp").GetInt64());
    }

    [Fact]
    public async Task WebSocket_PingPongMeasuresLatency()
    {
        using var ws = await ConnectAsync();
        var timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

        var ping = JsonSerializer.Serialize(new { type = "ping", timestamp });
        var response = await SendAndReceiveAsync(ws, ping);

        using var doc = JsonDocument.Parse(response);
        var root = doc.RootElement;
        var returnedTimestamp = root.GetProperty("timestamp").GetInt64();
        var latency = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() - returnedTimestamp;

        Assert.Equal(timestamp, returnedTimestamp);
        Assert.True(latency >= 0, "Latency should be non-negative");
    }
}
