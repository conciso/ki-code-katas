using System.Collections.Concurrent;
using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using Microsoft.Extensions.Logging;

namespace jets_backend_dotnet;

public class GameManager
{
    private const int ServerTickRate = 30;

    private readonly ILogger<GameManager> _logger;
    private readonly ConcurrentDictionary<string, Player> _players = new();
    private readonly ConcurrentDictionary<string, Lobby> _lobbies = new();
    private readonly ConcurrentDictionary<string, string> _playerLobby = new(); // playerId -> lobbyCode
    private readonly ConcurrentDictionary<string, GameSession> _lobbySessions = new(); // lobbyCode -> GameSession

    public GameManager(ILogger<GameManager> logger)
    {
        _logger = logger;
    }

    public IEnumerable<object> GetLobbies()
    {
        return _lobbies.Values.Select(lobby => new
        {
            code = lobby.Code,
            hostId = lobby.HostId,
            gameMode = lobby.GameMode,
            playerCount = lobby.Players.Count,
            status = _lobbySessions.ContainsKey(lobby.Code) ? "in_game" : "waiting",
            players = lobby.Players.Select(p => new { id = p.Id, name = p.Name, ready = p.Ready })
        });
    }

    public async Task HandleConnectionAsync(WebSocket socket, string playerName)
    {
        var playerId = GeneratePlayerId();
        var player = new Player(playerId, playerName, socket);
        _players[playerId] = player;

        _logger.LogInformation("Player connected: {PlayerName} ({PlayerId}), total: {Count}", playerName, playerId, _players.Count);

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
            _logger.LogInformation("Player disconnected: {PlayerName} ({PlayerId}), total: {Count}", playerName, playerId, _players.Count);
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

                case "RETURN_TO_LOBBY":
                    await HandleReturnToLobby(player);
                    break;

                default:
                    _logger.LogWarning("Unknown message type '{Type}' from {PlayerId}", type, player.Id);
                    await SendAsync(player.Socket, ErrorResponse("INVALID_MESSAGE", $"Unknown message type: {type}"));
                    break;
            }
        }
        catch (JsonException)
        {
            _logger.LogWarning("Invalid JSON from {PlayerId}: {Message}", player.Id, message);
            await SendAsync(player.Socket, ErrorResponse("INVALID_MESSAGE", "Message is not valid JSON"));
        }
        catch (KeyNotFoundException)
        {
            _logger.LogWarning("Missing fields in message from {PlayerId}: {Message}", player.Id, message);
            await SendAsync(player.Socket, ErrorResponse("INVALID_MESSAGE", "Message is missing required fields"));
        }
    }

    private async Task HandleCreateLobby(Player player)
    {
        var lobby = new Lobby(player);
        _lobbies[lobby.Code] = lobby;
        _playerLobby[player.Id] = lobby.Code;

        _logger.LogInformation("Lobby created: {LobbyCode} by {PlayerName} ({PlayerId})", lobby.Code, player.Name, player.Id);

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
            _logger.LogWarning("Lobby not found: {LobbyCode} (requested by {PlayerId})", lobbyCode, player.Id);
            await SendAsync(player.Socket, ErrorResponse("LOBBY_NOT_FOUND", "Lobby-Code existiert nicht"));
            return;
        }

        if (_lobbySessions.ContainsKey(lobbyCode))
        {
            _logger.LogWarning("Join denied, game in progress: {LobbyCode} (requested by {PlayerId})", lobbyCode, player.Id);
            await SendAsync(player.Socket, ErrorResponse("GAME_IN_PROGRESS", "Spiel hat bereits begonnen"));
            return;
        }

        if (lobby.IsFull)
        {
            _logger.LogWarning("Join denied, lobby full: {LobbyCode} (requested by {PlayerId})", lobbyCode, player.Id);
            await SendAsync(player.Socket, ErrorResponse("LOBBY_FULL", "Die Lobby ist voll (max. 4 Spieler)"));
            return;
        }

        lobby.AddPlayer(player);
        _playerLobby[player.Id] = lobbyCode;

        _logger.LogInformation("Player {PlayerName} ({PlayerId}) joined lobby {LobbyCode}, players: {Count}", player.Name, player.Id, lobbyCode, lobby.Players.Count);

        await SendLobbyState(lobby);
    }

    private async Task HandleLeaveLobby(Player player)
    {
        if (!TryGetPlayerLobby(player, out var lobby))
            return;

        lobby.RemovePlayer(player.Id);
        _playerLobby.TryRemove(player.Id, out _);

        _logger.LogInformation("Player {PlayerName} ({PlayerId}) left lobby {LobbyCode}, players: {Count}", player.Name, player.Id, lobby.Code, lobby.Players.Count);

        if (lobby.Players.Count == 0)
        {
            _lobbies.TryRemove(lobby.Code, out _);
            _logger.LogInformation("Lobby {LobbyCode} removed (empty)", lobby.Code);
        }
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
            _logger.LogWarning("Start denied, not host: {PlayerId} in lobby {LobbyCode}", player.Id, lobby.Code);
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

        _logger.LogInformation("Game starting in lobby {LobbyCode} with {Count} players", lobby.Code, lobby.Players.Count);

        var session = new GameSession(
            lobby.Players.Select(p => (p.Id, p.Name, p.Color)).ToList());

        var gameStarting = new
        {
            type = "GAME_STARTING",
            data = new
            {
                countdown = 3,
                gameMode = lobby.GameMode,
                fieldWidth = (int)GameSession.FieldWidth,
                fieldHeight = (int)GameSession.FieldHeight,
                players = session.Players.Select(p => new
                {
                    id = p.Id,
                    name = p.Name,
                    color = p.Color,
                    spawnX = p.X,
                    spawnY = p.Y
                })
            }
        };

        foreach (var lp in lobby.Players)
        {
            if (_players.TryGetValue(lp.Id, out var cp) && cp.Socket.State == WebSocketState.Open)
                await SendAsync(cp.Socket, gameStarting);
        }
        _lobbySessions[lobby.Code] = session;

        _ = RunGameLoop(session, lobby);
    }

    private async Task HandleReturnToLobby(Player player)
    {
        if (!TryGetPlayerLobby(player, out var lobby))
            return;

        if (lobby.HostId != player.Id)
        {
            _logger.LogWarning("Return to lobby denied, not host: {PlayerId} in lobby {LobbyCode}", player.Id, lobby.Code);
            await SendAsync(player.Socket, ErrorResponse("NOT_HOST", "Nur der Host darf diese Aktion ausführen"));
            return;
        }

        // Stop game session
        if (_lobbySessions.TryRemove(lobby.Code, out var session))
            session.Stop();

        // Reset ready status
        foreach (var lp in lobby.Players)
            lp.Ready = false;

        _logger.LogInformation("Returned to lobby {LobbyCode}", lobby.Code);
        await SendLobbyState(lobby);
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
        _logger.LogInformation("Game loop started for lobby {LobbyCode}", lobby.Code);
        var tickInterval = TimeSpan.FromMilliseconds(1000.0 / ServerTickRate);

        while (session.IsRunning)
        {
            var events = session.Tick();
            await BroadcastGameState(session, lobby);

            foreach (var evt in events)
            {
                await BroadcastToLobby(lobby, new { type = "GAME_EVENT", data = evt });
            }

            await Task.Delay(tickInterval);
        }

        _logger.LogInformation("Game over in lobby {LobbyCode}: {Reason} after {Ticks} ticks", lobby.Code, session.GameOverReason, session.TickCount);

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
                    activePowerUp = (string?)null,
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

    private async Task BroadcastToLobby(Lobby lobby, object message)
    {
        foreach (var lp in lobby.Players)
        {
            if (_players.TryGetValue(lp.Id, out var cp) && cp.Socket.State == WebSocketState.Open)
                await SendAsync(cp.Socket, message);
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
