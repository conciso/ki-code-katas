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
    private readonly ConcurrentDictionary<string, string> _playerLobby = new(); // playerId -> lobbyCode
    private readonly ConcurrentDictionary<string, GameSession> _lobbySessions = new(); // lobbyCode -> GameSession

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

                case "LEAVE_LOBBY":
                    await HandleLeaveLobby(player);
                    break;

                case "PLAYER_READY":
                    var ready = doc.RootElement.GetProperty("data").GetProperty("ready").GetBoolean();
                    await HandlePlayerReady(player, ready);
                    break;

                case "START_GAME":
                    await HandleStartGame(player);
                    break;

                case "PLAYER_INPUT":
                    HandlePlayerInput(player, doc.RootElement.GetProperty("data"));
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
        _playerLobby[player.Id] = lobby.Code;

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

        if (_lobbySessions.ContainsKey(lobbyCode))
        {
            await SendAsync(player.Socket, ErrorResponse("GAME_IN_PROGRESS", "Spiel hat bereits begonnen"));
            return;
        }

        if (lobby.IsFull)
        {
            await SendAsync(player.Socket, ErrorResponse("LOBBY_FULL", "Die Lobby ist voll (max. 4 Spieler)"));
            return;
        }

        lobby.AddPlayer(player);
        _playerLobby[player.Id] = lobbyCode;
        await SendLobbyState(lobby);
    }

    private async Task HandleLeaveLobby(Player player)
    {
        if (!TryGetPlayerLobby(player, out var lobby))
            return;

        lobby.RemovePlayer(player.Id);
        _playerLobby.TryRemove(player.Id, out _);

        if (lobby.Players.Count == 0)
            _lobbies.TryRemove(lobby.Code, out _);
        else
            await SendLobbyState(lobby);
    }

    private async Task HandlePlayerReady(Player player, bool ready)
    {
        if (!TryGetPlayerLobby(player, out var lobby))
            return;

        var lobbyPlayer = lobby.Players.FirstOrDefault(p => p.Id == player.Id);
        if (lobbyPlayer != null)
        {
            lobbyPlayer.Ready = ready;
            await SendLobbyState(lobby);
        }
    }

    private async Task HandleStartGame(Player player)
    {
        if (!TryGetPlayerLobby(player, out var lobby))
            return;

        if (lobby.HostId != player.Id)
        {
            await SendAsync(player.Socket, ErrorResponse("NOT_HOST", "Nur der Host darf das Spiel starten"));
            return;
        }

        if (lobby.Players.Count < 2)
        {
            await SendAsync(player.Socket, ErrorResponse("INVALID_MESSAGE", "Mindestens 2 Spieler erforderlich"));
            return;
        }

        if (!lobby.Players.All(p => p.Ready))
        {
            await SendAsync(player.Socket, ErrorResponse("INVALID_MESSAGE", "Nicht alle Spieler sind bereit"));
            return;
        }

        var gameStarting = new
        {
            type = "GAME_STARTING",
            data = new
            {
                countdown = 3,
                gameMode = lobby.GameMode,
                players = lobby.Players.Select(p => new
                {
                    id = p.Id,
                    name = p.Name,
                    color = p.Color
                })
            }
        };

        foreach (var lp in lobby.Players)
        {
            if (_players.TryGetValue(lp.Id, out var cp) && cp.Socket.State == WebSocketState.Open)
                await SendAsync(cp.Socket, gameStarting);
        }

        var session = new GameSession(
            lobby.Players.Select(p => (p.Id, p.Name, p.Color)).ToList());
        _lobbySessions[lobby.Code] = session;

        _ = RunGameLoop(session, lobby);
    }

    private void HandlePlayerInput(Player player, JsonElement data)
    {
        if (!_playerLobby.TryGetValue(player.Id, out var code))
            return;
        if (!_lobbySessions.TryGetValue(code, out var session))
            return;

        session.SetInput(player.Id, new PlayerInput
        {
            Left = data.GetProperty("left").GetBoolean(),
            Right = data.GetProperty("right").GetBoolean(),
            Up = data.GetProperty("up").GetBoolean(),
            Down = data.GetProperty("down").GetBoolean(),
            Shoot = data.GetProperty("shoot").GetBoolean(),
            Seq = data.GetProperty("seq").GetInt32()
        });
    }

    private async Task RunGameLoop(GameSession session, Lobby lobby)
    {
        var tickInterval = TimeSpan.FromMilliseconds(1000.0 / ServerTickRate);

        while (session.IsRunning)
        {
            var events = session.Tick();
            await BroadcastGameState(session, lobby);
            await Task.Delay(tickInterval);
        }

        await BroadcastGameOver(session, lobby);
        _lobbySessions.TryRemove(lobby.Code, out _);
    }

    private async Task BroadcastGameState(GameSession session, Lobby lobby)
    {
        var state = new
        {
            type = "GAME_STATE",
            data = new
            {
                tick = session.TickCount,
                players = session.Players.Select(p => new
                {
                    id = p.Id,
                    x = p.X,
                    y = p.Y,
                    hp = p.HP,
                    score = p.Score,
                    alive = p.Alive,
                    respawnIn = p.RespawnIn,
                    invincible = p.Invincible,
                    lastProcessedInput = p.LastProcessedInput
                }),
                projectiles = session.Projectiles.Select(p => new
                {
                    id = p.Id,
                    x = p.X,
                    y = p.Y,
                    vx = p.Vx,
                    vy = p.Vy,
                    owner = p.Owner
                })
            }
        };

        foreach (var lp in lobby.Players)
        {
            if (_players.TryGetValue(lp.Id, out var cp) && cp.Socket.State == WebSocketState.Open)
                await SendAsync(cp.Socket, state);
        }
    }

    private async Task BroadcastGameOver(GameSession session, Lobby lobby)
    {
        var gameOver = new
        {
            type = "GAME_OVER",
            data = new
            {
                reason = session.GameOverReason,
                finalScores = session.FinalScores.Select(s => new
                {
                    playerId = s.PlayerId,
                    name = s.Name,
                    score = s.Score,
                    kills = s.Kills
                })
            }
        };

        foreach (var lp in lobby.Players)
        {
            if (_players.TryGetValue(lp.Id, out var cp) && cp.Socket.State == WebSocketState.Open)
                await SendAsync(cp.Socket, gameOver);
        }
    }

    private bool TryGetPlayerLobby(Player player, out Lobby lobby)
    {
        lobby = null!;
        return _playerLobby.TryGetValue(player.Id, out var code) && _lobbies.TryGetValue(code, out lobby!);
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
