using System.Net.WebSockets;
using System.Text;
using Microsoft.AspNetCore.Mvc.Testing;

namespace jets_backend_dotnet.Tests;

public class WebSocketTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly WebApplicationFactory<Program> _factory;

    public WebSocketTests(WebApplicationFactory<Program> factory)
    {
        _factory = factory;
    }

    [Fact]
    public async Task WebSocket_ConnectsSuccessfully()
    {
        var client = _factory.Server.CreateWebSocketClient();

        var ws = await client.ConnectAsync(new Uri("ws://localhost/ws"), CancellationToken.None);

        Assert.Equal(WebSocketState.Open, ws.State);
        ws.Dispose();
    }

    [Fact]
    public async Task WebSocket_EchoesMessage()
    {
        var client = _factory.Server.CreateWebSocketClient();
        var ws = await client.ConnectAsync(new Uri("ws://localhost/ws"), CancellationToken.None);

        var message = "Hello, WebSocket!";
        var sendBuffer = Encoding.UTF8.GetBytes(message);
        await ws.SendAsync(sendBuffer, WebSocketMessageType.Text, true, CancellationToken.None);

        var receiveBuffer = new byte[1024];
        var result = await ws.ReceiveAsync(receiveBuffer, CancellationToken.None);

        var received = Encoding.UTF8.GetString(receiveBuffer, 0, result.Count);
        Assert.Equal(message, received);
        Assert.Equal(WebSocketMessageType.Text, result.MessageType);

        ws.Dispose();
    }
}
