using System.Collections.Concurrent;
using System.Net.WebSockets;
using System.Text;
using System.Text.Json;

namespace jets_backend_dotnet;

public class GameManager
{
    private const int ServerTickRate = 30;

    private readonly ConcurrentDictionary<string, Player> _players = new();
    private readonly ConcurrentDictionary<string, Lobby> _lobbies = new();

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
                await HandleMessageAsync(player, message);
            }
        }
        catch (WebSocketException) { }
    }

    private async Task HandleMessageAsync(Player player, string message)
    {
        try
        {
            using var doc = JsonDocument.Parse(message);
            var type = doc.RootElement.GetProperty("type").GetString();

            switch (type)
            {
                case "PING":
                    var timestamp = doc.RootElement.GetProperty("data").GetProperty("timestamp").GetInt64();
                    await SendAsync(player.Socket, new { type = "PONG", data = new { timestamp } });
                    break;

                case "CREATE_LOBBY":
                    await HandleCreateLobby(player);
                    break;

                case "JOIN_LOBBY":
                    var lobbyCode = doc.RootElement.GetProperty("data").GetProperty("lobbyCode").GetString()!;
                    await HandleJoinLobby(player, lobbyCode);
                    break;

                default:
                    await SendAsync(player.Socket, ErrorResponse("INVALID_MESSAGE", $"Unknown message type: {type}"));
                    break;
            }
        }
        catch (JsonException)
        {
            await SendAsync(player.Socket, ErrorResponse("INVALID_MESSAGE", "Message is not valid JSON"));
        }
        catch (KeyNotFoundException)
        {
            await SendAsync(player.Socket, ErrorResponse("INVALID_MESSAGE", "Message is missing required fields"));
        }
    }

    private async Task HandleCreateLobby(Player player)
    {
        var lobby = new Lobby(player);
        _lobbies[lobby.Code] = lobby;

        await SendAsync(player.Socket, new
        {
            type = "LOBBY_CREATED",
            data = new { lobbyCode = lobby.Code, hostId = lobby.HostId }
        });

        await SendLobbyState(lobby);
    }

    private async Task HandleJoinLobby(Player player, string lobbyCode)
    {
        if (!_lobbies.TryGetValue(lobbyCode, out var lobby))
        {
            await SendAsync(player.Socket, ErrorResponse("LOBBY_NOT_FOUND", "Lobby-Code existiert nicht"));
            return;
        }

        if (lobby.IsFull)
        {
            await SendAsync(player.Socket, ErrorResponse("LOBBY_FULL", "Die Lobby ist voll (max. 4 Spieler)"));
            return;
        }

        lobby.AddPlayer(player);
        await SendLobbyState(lobby);
    }

    private async Task SendLobbyState(Lobby lobby)
    {
        var state = new
        {
            type = "LOBBY_STATE",
            data = new
            {
                lobbyCode = lobby.Code,
                hostId = lobby.HostId,
                gameMode = lobby.GameMode,
                players = lobby.Players.Select(p => new
                {
                    id = p.Id,
                    name = p.Name,
                    ready = p.Ready,
                    color = p.Color
                })
            }
        };

        foreach (var lobbyPlayer in lobby.Players)
        {
            if (_players.TryGetValue(lobbyPlayer.Id, out var connectedPlayer)
                && connectedPlayer.Socket.State == WebSocketState.Open)
            {
                await SendAsync(connectedPlayer.Socket, state);
            }
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
