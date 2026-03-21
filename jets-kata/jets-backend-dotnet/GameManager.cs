using System.Collections.Concurrent;
using System.Net.WebSockets;
using System.Text;
using System.Text.Json;

namespace jets_backend_dotnet;

public class GameManager
{
    private const int ServerTickRate = 30;

    private readonly ConcurrentDictionary<string, Player> _players = new();

    public async Task HandleConnectionAsync(WebSocket socket, string playerName)
    {
        var playerId = GeneratePlayerId();
        var player = new Player(playerId, playerName, socket);
        _players[playerId] = player;

        await SendAsync(socket, new
        {
            type = "CONNECTED",
            data = new { playerId, serverTickRate = ServerTickRate }
        });

        try
        {
            await ReceiveLoop(player);
        }
        finally
        {
            _players.TryRemove(playerId, out _);
            await BroadcastAsync(new
            {
                type = "DISCONNECTED",
                data = new { playerId, playerName }
            });
        }
    }

    private async Task ReceiveLoop(Player player)
    {
        var buffer = new byte[4096];

        try
        {
            while (player.Socket.State == WebSocketState.Open)
            {
                var result = await player.Socket.ReceiveAsync(buffer, CancellationToken.None);
                if (result.MessageType == WebSocketMessageType.Close)
                    break;

                var message = Encoding.UTF8.GetString(buffer, 0, result.Count);
                var response = HandleMessage(message);
                await SendAsync(player.Socket, response);
            }
        }
        catch (WebSocketException) { }
    }

    private static object HandleMessage(string message)
    {
        try
        {
            using var doc = JsonDocument.Parse(message);
            var type = doc.RootElement.GetProperty("type").GetString();

            return type switch
            {
                "PING" => new
                {
                    type = "PONG",
                    data = new { timestamp = doc.RootElement.GetProperty("data").GetProperty("timestamp").GetInt64() }
                },
                _ => ErrorResponse("INVALID_MESSAGE", $"Unknown message type: {type}")
            };
        }
        catch (JsonException)
        {
            return ErrorResponse("INVALID_MESSAGE", "Message is not valid JSON");
        }
        catch (KeyNotFoundException)
        {
            return ErrorResponse("INVALID_MESSAGE", "Message is missing required fields");
        }
    }

    private static object ErrorResponse(string code, string message) =>
        new { type = "ERROR", data = new { code, message } };

    private async Task BroadcastAsync(object message)
    {
        foreach (var player in _players.Values)
        {
            if (player.Socket.State == WebSocketState.Open)
                await SendAsync(player.Socket, message);
        }
    }

    private static async Task SendAsync(WebSocket socket, object message)
    {
        var json = JsonSerializer.Serialize(message);
        var bytes = Encoding.UTF8.GetBytes(json);
        await socket.SendAsync(bytes, WebSocketMessageType.Text, true, CancellationToken.None);
    }

    private static string GeneratePlayerId() =>
        "p" + Guid.NewGuid().ToString("N")[..7];
}
