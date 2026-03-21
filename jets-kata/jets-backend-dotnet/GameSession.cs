namespace jets_backend_dotnet;

public class GameSession
{
    public const double FieldWidth = 1920;
    public const double FieldHeight = 1080;
    public const double DefaultSpeed = 5.0;
    public const double MinSpeed = 2.0;
    public const double MaxSpeed = 8.0;
    public const double TurnRate = 0.08;
    public const double Acceleration = 0.2;
    public const double ProjectileSpeed = 10.0;
    public const int ShootCooldownTicks = 6;
    public const double CollisionRadius = 15.0;
    public const int HitScore = 100;
    public const int KillBonus = 500;
    public const int DeathPenalty = 200;
    public const int RespawnTicks = 90;
    public const int InvincibilityTicks = 60;
    public const int GameDurationTicks = 3600; // 2 Minuten bei 30Hz
    public const int ScoreLimit = 5000;

    private readonly HashSet<string> _activePlayers = new();

    public List<PlayerState> Players { get; }
    public List<Projectile> Projectiles { get; } = [];
    public bool IsRunning { get; private set; } = true;
    public int TickCount { get; private set; }
    public string? GameOverReason { get; private set; }
    public List<FinalScore> FinalScores { get; private set; } = [];

    public GameSession(List<(string Id, string Name, string Color)> players)
    {
        Players = players.Select((p, i) => new PlayerState(
            p.Id, p.Name, p.Color,
            SpawnX(i, players.Count),
            SpawnY(i, players.Count),
            SpawnAngle(i, players.Count)
        )).ToList();

        foreach (var p in Players)
            _activePlayers.Add(p.Id);
    }

    public void RemovePlayer(string playerId)
    {
        _activePlayers.Remove(playerId);
    }

    public void Stop()
    {
        IsRunning = false;
    }

    public void SetInput(string playerId, PlayerInput input)
    {
        var player = Players.FirstOrDefault(p => p.Id == playerId);
        if (player != null)
            player.Input = input;
    }

    public List<object> Tick()
    {
        if (!IsRunning)
            return [];

        TickCount++;
        var events = new List<object>();

        foreach (var player in Players)
        {
            if (player.Alive)
            {
                ProcessInput(player);
                MovePlayer(player);
                ProcessShooting(player);
                ProcessInvincibility(player);
            }
            else
            {
                ProcessRespawn(player, events);
            }
        }

        MoveProjectiles();
        CheckCollisions(events);
        RemoveOutOfBoundsProjectiles();
        CheckGameOver();

        return events;
    }

    private static void ProcessInput(PlayerState player)
    {
        if (player.Input.Left)
            player.Angle -= TurnRate;
        if (player.Input.Right)
            player.Angle += TurnRate;
        if (player.Input.Up)
            player.Speed = Math.Min(player.Speed + Acceleration, MaxSpeed);
        if (player.Input.Down)
            player.Speed = Math.Max(player.Speed - Acceleration, MinSpeed);

        player.LastProcessedInput = player.Input.Seq;
    }

    private static void MovePlayer(PlayerState player)
    {
        player.X += Math.Cos(player.Angle) * player.Speed;
        player.Y += Math.Sin(player.Angle) * player.Speed;

        player.X = Math.Clamp(player.X, 0, FieldWidth);
        player.Y = Math.Clamp(player.Y, 0, FieldHeight);
    }

    private void ProcessShooting(PlayerState player)
    {
        if (player.ShootCooldown > 0)
            player.ShootCooldown--;

        if (player.Input.Shoot && player.ShootCooldown == 0)
        {
            var vx = Math.Cos(player.Angle) * ProjectileSpeed;
            var vy = Math.Sin(player.Angle) * ProjectileSpeed;
            Projectiles.Add(new Projectile(player.X, player.Y, vx, vy, player.Id));
            player.ShootCooldown = ShootCooldownTicks;
        }
    }

    private void ProcessRespawn(PlayerState player, List<object> events)
    {
        if (player.RespawnIn <= 0)
            return;

        player.RespawnIn--;

        if (player.RespawnIn == 0)
        {
            player.Alive = true;
            player.HP = 3;
            player.Invincible = true;
            player.InvincibleTicks = InvincibilityTicks;
            player.Speed = DefaultSpeed;

            var index = Players.IndexOf(player);
            player.X = SpawnX(index, Players.Count);
            player.Y = SpawnY(index, Players.Count);
            player.Angle = SpawnAngle(index, Players.Count);

            events.Add(new { @event = "PLAYER_RESPAWN", playerId = player.Id, x = player.X, y = player.Y });
        }
    }

    private static void ProcessInvincibility(PlayerState player)
    {
        if (!player.Invincible)
            return;

        player.InvincibleTicks--;
        if (player.InvincibleTicks <= 0)
            player.Invincible = false;
    }

    private void MoveProjectiles()
    {
        foreach (var proj in Projectiles)
        {
            proj.X += proj.Vx;
            proj.Y += proj.Vy;
        }
    }

    private void CheckCollisions(List<object> events)
    {
        var toRemove = new List<Projectile>();

        foreach (var proj in Projectiles)
        {
            foreach (var player in Players.Where(p => p.Alive && p.Id != proj.Owner && !p.Invincible))
            {
                var dx = proj.X - player.X;
                var dy = proj.Y - player.Y;
                if (dx * dx + dy * dy > CollisionRadius * CollisionRadius)
                    continue;

                toRemove.Add(proj);
                player.HP--;

                var shooter = Players.FirstOrDefault(p => p.Id == proj.Owner);
                if (shooter != null)
                    shooter.Score += HitScore;

                events.Add(new { @event = "PLAYER_HIT", playerId = player.Id, damage = 1, hitBy = proj.Owner });

                if (player.HP <= 0)
                {
                    player.Alive = false;
                    player.RespawnIn = RespawnTicks;
                    player.Score -= DeathPenalty;
                    if (shooter != null)
                    {
                        shooter.Score += KillBonus;
                        shooter.Kills++;
                    }
                    events.Add(new { @event = "PLAYER_KILLED", playerId = player.Id, killedBy = proj.Owner });
                }

                break; // Projektil kann nur einen Spieler treffen
            }
        }

        foreach (var proj in toRemove)
            Projectiles.Remove(proj);
    }

    private void RemoveOutOfBoundsProjectiles()
    {
        Projectiles.RemoveAll(p =>
            p.X < 0 || p.X > FieldWidth || p.Y < 0 || p.Y > FieldHeight);
    }

    private void CheckGameOver()
    {
        if (_activePlayers.Count <= 1)
            EndGame("ALL_LEFT");
        else if (TickCount >= GameDurationTicks)
            EndGame("TIME_UP");
        else if (Players.Any(p => p.Score >= ScoreLimit))
            EndGame("SCORE_REACHED");
    }

    private void EndGame(string reason)
    {
        IsRunning = false;
        GameOverReason = reason;
        FinalScores = Players
            .OrderByDescending(p => p.Score)
            .Select(p => new FinalScore(p.Id, p.Name, p.Score, p.Kills))
            .ToList();
    }

    private static readonly (double X, double Y)[] SpawnPositions =
    [
        (FieldWidth * 0.2, FieldHeight * 0.2),   // oben links
        (FieldWidth * 0.8, FieldHeight * 0.8),   // unten rechts
        (FieldWidth * 0.8, FieldHeight * 0.2),   // oben rechts
        (FieldWidth * 0.2, FieldHeight * 0.8),   // unten links
    ];

    private static double SpawnX(int index, int count) => SpawnPositions[index % SpawnPositions.Length].X;

    private static double SpawnY(int index, int count) => SpawnPositions[index % SpawnPositions.Length].Y;

    private static double SpawnAngle(int index, int count)
    {
        // Spieler schaut zur Feldmitte
        var spawnX = SpawnX(index, count);
        var spawnY = SpawnY(index, count);
        return Math.Atan2(FieldHeight / 2 - spawnY, FieldWidth / 2 - spawnX);
    }
}
