using System.Net.WebSockets;
using jets_backend_dotnet;

namespace jets_backend_dotnet.Tests;

public class PlayerTests
{
    [Fact]
    public void Player_StoresId()
    {
        var player = new Player("p1234567", "Alice", new ClientWebSocket());

        Assert.Equal("p1234567", player.Id);
    }

    [Fact]
    public void Player_StoresName()
    {
        var player = new Player("p1234567", "Alice", new ClientWebSocket());

        Assert.Equal("Alice", player.Name);
    }

    [Fact]
    public void Player_StoresSocket()
    {
        var socket = new ClientWebSocket();
        var player = new Player("p1234567", "Alice", socket);

        Assert.Same(socket, player.Socket);
    }
}
