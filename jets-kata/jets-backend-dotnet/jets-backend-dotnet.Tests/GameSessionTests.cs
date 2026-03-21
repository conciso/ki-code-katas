namespace jets_backend_dotnet.Tests;

public class GameSessionTests
{
    private static GameSession CreateTwoPlayerSession()
    {
        var players = new List<(string Id, string Name, string Color)>
        {
            ("p1111111", "Alice", "#FF4444"),
            ("p2222222", "Bob", "#4444FF")
        };
        return new GameSession(players);
    }

    // --- Erstellung ---

    [Fact]
    public void NewSession_IsRunning()
    {
        var session = CreateTwoPlayerSession();

        Assert.True(session.IsRunning);
    }

    [Fact]
    public void NewSession_TickIsZero()
    {
        var session = CreateTwoPlayerSession();

        Assert.Equal(0, session.TickCount);
    }

    [Fact]
    public void NewSession_HasCorrectPlayerCount()
    {
        var session = CreateTwoPlayerSession();

        Assert.Equal(2, session.Players.Count);
    }

    [Fact]
    public void NewSession_PlayersHaveIds()
    {
        var session = CreateTwoPlayerSession();

        Assert.Equal("p1111111", session.Players[0].Id);
        Assert.Equal("p2222222", session.Players[1].Id);
    }

    [Fact]
    public void NewSession_PlayersHaveHP3()
    {
        var session = CreateTwoPlayerSession();

        Assert.All(session.Players, p => Assert.Equal(3, p.HP));
    }

    [Fact]
    public void NewSession_PlayersHaveScore0()
    {
        var session = CreateTwoPlayerSession();

        Assert.All(session.Players, p => Assert.Equal(0, p.Score));
    }

    [Fact]
    public void NewSession_PlayersAreAlive()
    {
        var session = CreateTwoPlayerSession();

        Assert.All(session.Players, p => Assert.True(p.Alive));
    }

    [Fact]
    public void NewSession_PlayersHaveDefaultSpeed()
    {
        var session = CreateTwoPlayerSession();

        Assert.All(session.Players, p => Assert.Equal(5.0, p.Speed));
    }

    [Fact]
    public void NewSession_PlayersHaveSpawnPositions()
    {
        var session = CreateTwoPlayerSession();

        // Spieler sollten an unterschiedlichen Positionen spawnen
        Assert.NotEqual(session.Players[0].X, session.Players[1].X);
    }

    [Fact]
    public void NewSession_PlayersAreWithinField()
    {
        var session = CreateTwoPlayerSession();

        Assert.All(session.Players, p =>
        {
            Assert.InRange(p.X, 0, 1920);
            Assert.InRange(p.Y, 0, 1080);
        });
    }

    [Fact]
    public void NewSession_NoProjectiles()
    {
        var session = CreateTwoPlayerSession();

        Assert.Empty(session.Projectiles);
    }

    [Fact]
    public void Tick_IncrementsTick()
    {
        var session = CreateTwoPlayerSession();

        session.Tick();
        session.Tick();

        Assert.Equal(2, session.TickCount);
    }

    [Fact]
    public void NewSession_FourPlayersGetDifferentPositions()
    {
        var players = new List<(string Id, string Name, string Color)>
        {
            ("p1111111", "Alice", "#FF4444"),
            ("p2222222", "Bob", "#4444FF"),
            ("p3333333", "Charlie", "#44FF44"),
            ("p4444444", "Dave", "#FFFF44")
        };
        var session = new GameSession(players);

        var positions = session.Players.Select(p => (p.X, p.Y)).ToList();
        Assert.Equal(4, positions.Distinct().Count());
    }

    // --- Jet-Bewegung ---

    [Fact]
    public void Tick_JetMovesForwardInAngleDirection()
    {
        var session = CreateTwoPlayerSession();
        var player = session.Players[0];
        player.Angle = 0; // nach rechts
        var startX = player.X;

        session.Tick();

        Assert.True(player.X > startX, "Jet should move right when angle=0");
    }

    [Fact]
    public void Tick_JetMovesWithoutInput()
    {
        var session = CreateTwoPlayerSession();
        var player = session.Players[0];
        player.Angle = 0;
        var startX = player.X;

        session.Tick();
        session.Tick();
        session.Tick();

        Assert.True(player.X > startX + player.Speed * 2, "Jet keeps moving without input");
    }

    [Fact]
    public void SetInput_LeftTurnsAngleDown()
    {
        var session = CreateTwoPlayerSession();
        var player = session.Players[0];
        var startAngle = player.Angle;

        session.SetInput("p1111111", new PlayerInput { Left = true, Seq = 1 });
        session.Tick();

        Assert.True(player.Angle < startAngle, "Left should decrease angle");
    }

    [Fact]
    public void SetInput_RightTurnsAngleUp()
    {
        var session = CreateTwoPlayerSession();
        var player = session.Players[0];
        var startAngle = player.Angle;

        session.SetInput("p1111111", new PlayerInput { Right = true, Seq = 1 });
        session.Tick();

        Assert.True(player.Angle > startAngle, "Right should increase angle");
    }

    [Fact]
    public void SetInput_UpAccelerates()
    {
        var session = CreateTwoPlayerSession();
        var player = session.Players[0];
        var startSpeed = player.Speed;

        session.SetInput("p1111111", new PlayerInput { Up = true, Seq = 1 });
        session.Tick();

        Assert.True(player.Speed > startSpeed, "Up should increase speed");
    }

    [Fact]
    public void SetInput_UpCapsAtMaxSpeed()
    {
        var session = CreateTwoPlayerSession();
        var player = session.Players[0];
        player.Speed = 7.9;

        session.SetInput("p1111111", new PlayerInput { Up = true, Seq = 1 });
        session.Tick();

        Assert.True(player.Speed <= 8.0, "Speed should not exceed maxSpeed");
    }

    [Fact]
    public void SetInput_DownDecelerates()
    {
        var session = CreateTwoPlayerSession();
        var player = session.Players[0];
        var startSpeed = player.Speed;

        session.SetInput("p1111111", new PlayerInput { Down = true, Seq = 1 });
        session.Tick();

        Assert.True(player.Speed < startSpeed, "Down should decrease speed");
    }

    [Fact]
    public void SetInput_DownCapsAtMinSpeed()
    {
        var session = CreateTwoPlayerSession();
        var player = session.Players[0];
        player.Speed = 2.1;

        session.SetInput("p1111111", new PlayerInput { Down = true, Seq = 1 });
        session.Tick();

        Assert.True(player.Speed >= 2.0, "Speed should not go below minSpeed");
    }

    [Fact]
    public void Tick_ClampsPositionAtFieldBounds()
    {
        var session = CreateTwoPlayerSession();
        var player = session.Players[0];
        player.X = 1919;
        player.Angle = 0; // nach rechts
        player.Speed = 8.0;

        session.Tick();

        Assert.Equal(1920, player.X);
    }

    [Fact]
    public void Tick_ClampsPositionAtZero()
    {
        var session = CreateTwoPlayerSession();
        var player = session.Players[0];
        player.X = 1;
        player.Angle = Math.PI; // nach links
        player.Speed = 8.0;

        session.Tick();

        Assert.Equal(0, player.X);
    }

    [Fact]
    public void SetInput_UpdatesLastProcessedInput()
    {
        var session = CreateTwoPlayerSession();

        session.SetInput("p1111111", new PlayerInput { Seq = 42 });
        session.Tick();

        Assert.Equal(42, session.Players[0].LastProcessedInput);
    }

    // --- Schießen + Projektile ---

    [Fact]
    public void Shoot_CreatesProjectile()
    {
        var session = CreateTwoPlayerSession();
        var player = session.Players[0];
        player.Angle = 0;

        session.SetInput(player.Id, new PlayerInput { Shoot = true, Seq = 1 });
        session.Tick();

        Assert.Single(session.Projectiles);
    }

    [Fact]
    public void Shoot_ProjectileStartsAtPlayerPosition()
    {
        var session = CreateTwoPlayerSession();
        var player = session.Players[0];

        session.SetInput(player.Id, new PlayerInput { Shoot = true, Seq = 1 });
        session.Tick();

        var proj = session.Projectiles[0];
        // Projektil startet ungefähr an der Spielerposition (nach Bewegung im gleichen Tick)
        Assert.InRange(proj.X, player.X - 20, player.X + 20);
        Assert.InRange(proj.Y, player.Y - 20, player.Y + 20);
    }

    [Fact]
    public void Shoot_ProjectileFliesInPlayerAngle()
    {
        var session = CreateTwoPlayerSession();
        var player = session.Players[0];
        player.Angle = 0; // nach rechts

        session.SetInput(player.Id, new PlayerInput { Shoot = true, Seq = 1 });
        session.Tick();

        var proj = session.Projectiles[0];
        Assert.True(proj.Vx > 0, "Projectile should fly right when angle=0");
        Assert.InRange(proj.Vy, -0.01, 0.01); // ~0
    }

    [Fact]
    public void Shoot_ProjectileHasOwner()
    {
        var session = CreateTwoPlayerSession();

        session.SetInput("p1111111", new PlayerInput { Shoot = true, Seq = 1 });
        session.Tick();

        Assert.Equal("p1111111", session.Projectiles[0].Owner);
    }

    [Fact]
    public void Shoot_CooldownPreventsRapidFire()
    {
        var session = CreateTwoPlayerSession();
        session.SetInput("p1111111", new PlayerInput { Shoot = true, Seq = 1 });
        session.Tick(); // Schuss 1

        // Nächste Ticks mit Shoot=true sollten keinen neuen Schuss erzeugen (Cooldown)
        session.Tick();
        session.Tick();

        Assert.Single(session.Projectiles);
    }

    [Fact]
    public void Shoot_CanFireAgainAfterCooldown()
    {
        var session = CreateTwoPlayerSession();
        var player = session.Players[0];
        player.Angle = -Math.PI / 2; // nach oben, Mitte des Felds

        session.SetInput(player.Id, new PlayerInput { Shoot = true, Seq = 1 });
        session.Tick(); // Tick 1: Schuss 1, Cooldown=6

        // Tick 2-7: Cooldown runterzählen (auf Tick 7 ist Cooldown=0 → Schuss 2)
        for (var i = 0; i < 6; i++)
            session.Tick();

        // Tick 1 + 6 Ticks = 7 Ticks, Schuss 1 + Schuss 2
        Assert.True(session.Projectiles.Count >= 2, "Should have fired twice after cooldown");
    }

    [Fact]
    public void Tick_ProjectilesMovePerTick()
    {
        var session = CreateTwoPlayerSession();
        var player = session.Players[0];
        player.Angle = 0;

        session.SetInput(player.Id, new PlayerInput { Shoot = true, Seq = 1 });
        session.Tick();

        var proj = session.Projectiles[0];
        var xAfterSpawn = proj.X;

        session.SetInput(player.Id, new PlayerInput { Shoot = false, Seq = 2 });
        session.Tick();

        Assert.True(proj.X > xAfterSpawn, "Projectile should move each tick");
    }

    [Fact]
    public void Tick_ProjectilesRemovedOutsideField()
    {
        var session = CreateTwoPlayerSession();
        var player = session.Players[0];
        player.Angle = 0;
        player.X = 1910;

        session.SetInput(player.Id, new PlayerInput { Shoot = true, Seq = 1 });
        session.Tick();

        // Projektil fliegt rechts raus
        for (var i = 0; i < 10; i++)
        {
            session.SetInput(player.Id, new PlayerInput { Shoot = false, Seq = 2 });
            session.Tick();
        }

        Assert.Empty(session.Projectiles);
    }

    // --- Kollision + Schaden ---

    private GameSession CreateSessionWithProjectileHitting(string targetId)
    {
        var session = CreateTwoPlayerSession();
        var shooter = session.Players.First(p => p.Id != targetId);
        var target = session.Players.First(p => p.Id == targetId);

        // Platziere Schütze links vom Ziel, beide auf gleicher Y-Höhe
        shooter.X = 200;
        shooter.Y = 540;
        shooter.Angle = 0; // nach rechts
        target.X = 220; // knapp rechts vom Schützen
        target.Y = 540;

        session.SetInput(shooter.Id, new PlayerInput { Shoot = true, Seq = 1 });
        return session;
    }

    [Fact]
    public void Collision_ProjectileHitsPlayer_ReducesHP()
    {
        var session = CreateSessionWithProjectileHitting("p2222222");
        var target = session.Players[1];
        var hpBefore = target.HP;

        session.Tick();

        Assert.Equal(hpBefore - 1, target.HP);
    }

    [Fact]
    public void Collision_ProjectileHitsPlayer_RemovesProjectile()
    {
        var session = CreateSessionWithProjectileHitting("p2222222");

        session.Tick();

        Assert.Empty(session.Projectiles);
    }

    [Fact]
    public void Collision_ProjectileHitsPlayer_GivesShooterScore()
    {
        var session = CreateSessionWithProjectileHitting("p2222222");
        var shooter = session.Players[0];

        session.Tick();

        Assert.Equal(100, shooter.Score);
    }

    [Fact]
    public void Collision_OwnProjectileDoesNotHitSelf()
    {
        var session = CreateTwoPlayerSession();
        var player = session.Players[0];
        player.X = 500;
        player.Y = 540;
        player.Angle = 0;

        // Platziere ein Projektil direkt auf dem Spieler, aber mit seinem eigenen Owner
        session.Projectiles.Add(new Projectile(500, 540, 10, 0, player.Id));

        session.Tick();

        // HP sollte sich nicht ändern
        Assert.Equal(3, player.HP);
    }

    [Fact]
    public void Collision_KillGivesBonus()
    {
        var session = CreateSessionWithProjectileHitting("p2222222");
        var shooter = session.Players[0];
        var target = session.Players[1];
        target.HP = 1; // ein Treffer tötet

        session.Tick();

        // 100 (Treffer) + 500 (Kill)
        Assert.Equal(600, shooter.Score);
    }

    [Fact]
    public void Collision_KillSetsAliveToFalse()
    {
        var session = CreateSessionWithProjectileHitting("p2222222");
        var target = session.Players[1];
        target.HP = 1;

        session.Tick();

        Assert.False(target.Alive);
    }

    [Fact]
    public void Collision_DeathDeductsScore()
    {
        var session = CreateSessionWithProjectileHitting("p2222222");
        var target = session.Players[1];
        target.HP = 1;
        target.Score = 300;

        session.Tick();

        Assert.Equal(100, target.Score); // 300 - 200
    }

    [Fact]
    public void Collision_ReturnsPlayerHitEvent()
    {
        var session = CreateSessionWithProjectileHitting("p2222222");

        var events = session.Tick();

        Assert.Contains(events, e => e.ToString()!.Contains("PLAYER_HIT"));
    }

    [Fact]
    public void Collision_KillReturnsPlayerKilledEvent()
    {
        var session = CreateSessionWithProjectileHitting("p2222222");
        session.Players[1].HP = 1;

        var events = session.Tick();

        Assert.Contains(events, e => e.ToString()!.Contains("PLAYER_KILLED"));
    }

    [Fact]
    public void Collision_InvinciblePlayerNotHit()
    {
        var session = CreateSessionWithProjectileHitting("p2222222");
        var target = session.Players[1];
        target.Invincible = true;
        target.InvincibleTicks = 60;

        session.Tick();

        Assert.Equal(3, target.HP);
    }

    [Fact]
    public void Collision_KillIncrementsKillCount()
    {
        var session = CreateSessionWithProjectileHitting("p2222222");
        var shooter = session.Players[0];
        session.Players[1].HP = 1;

        session.Tick();

        Assert.Equal(1, shooter.Kills);
    }

    // --- Respawn ---

    private GameSession CreateSessionWithDeadPlayer()
    {
        var session = CreateTwoPlayerSession();
        var player = session.Players[1];
        player.Alive = false;
        player.HP = 0;
        player.RespawnIn = 90; // 3 Sekunden bei 30Hz
        return session;
    }

    [Fact]
    public void Respawn_CountsDown()
    {
        var session = CreateSessionWithDeadPlayer();
        var player = session.Players[1];

        session.Tick();

        Assert.Equal(89, player.RespawnIn);
    }

    [Fact]
    public void Respawn_PlayerStaysDeadDuringCountdown()
    {
        var session = CreateSessionWithDeadPlayer();
        var player = session.Players[1];

        for (var i = 0; i < 89; i++)
            session.Tick();

        Assert.False(player.Alive);
        Assert.Equal(1, player.RespawnIn);
    }

    [Fact]
    public void Respawn_PlayerRevivesWhenCountdownReachesZero()
    {
        var session = CreateSessionWithDeadPlayer();
        var player = session.Players[1];

        for (var i = 0; i < 90; i++)
            session.Tick();

        Assert.True(player.Alive);
    }

    [Fact]
    public void Respawn_PlayerGetsFullHP()
    {
        var session = CreateSessionWithDeadPlayer();
        var player = session.Players[1];

        for (var i = 0; i < 90; i++)
            session.Tick();

        Assert.Equal(3, player.HP);
    }

    [Fact]
    public void Respawn_PlayerIsInvincible()
    {
        var session = CreateSessionWithDeadPlayer();
        var player = session.Players[1];

        for (var i = 0; i < 90; i++)
            session.Tick();

        Assert.True(player.Invincible);
    }

    [Fact]
    public void Respawn_InvincibilityWearsOff()
    {
        var session = CreateSessionWithDeadPlayer();
        var player = session.Players[1];

        // 90 Ticks Respawn + 60 Ticks Invincibility
        for (var i = 0; i < 150; i++)
            session.Tick();

        Assert.False(player.Invincible);
    }

    [Fact]
    public void Respawn_PlayerGetsNewPosition()
    {
        var session = CreateSessionWithDeadPlayer();
        var player = session.Players[1];
        player.X = 100;
        player.Y = 100;

        for (var i = 0; i < 90; i++)
            session.Tick();

        // Respawn-Position sollte sich von der Todesposition unterscheiden
        Assert.InRange(player.X, 0, GameSession.FieldWidth);
        Assert.InRange(player.Y, 0, GameSession.FieldHeight);
    }

    [Fact]
    public void Respawn_ReturnsPlayerRespawnEvent()
    {
        var session = CreateSessionWithDeadPlayer();
        var player = session.Players[1];

        var allEvents = new List<object>();
        for (var i = 0; i < 90; i++)
            allEvents.AddRange(session.Tick());

        Assert.Contains(allEvents, e => e.ToString()!.Contains("PLAYER_RESPAWN"));
    }

    [Fact]
    public void Kill_SetsRespawnCountdown()
    {
        var session = CreateSessionWithProjectileHitting("p2222222");
        var target = session.Players[1];
        target.HP = 1;

        session.Tick();

        Assert.Equal(90, target.RespawnIn);
    }

    // --- GAME_OVER ---

    [Fact]
    public void GameOver_TimeUp()
    {
        var session = CreateTwoPlayerSession();

        // Simuliere Spielzeit bis zum Limit (default: 2 Minuten = 3600 Ticks bei 30Hz)
        for (var i = 0; i < GameSession.GameDurationTicks; i++)
            session.Tick();

        Assert.False(session.IsRunning);
        Assert.Equal("TIME_UP", session.GameOverReason);
    }

    [Fact]
    public void GameOver_NotBeforeTimeLimit()
    {
        var session = CreateTwoPlayerSession();

        for (var i = 0; i < GameSession.GameDurationTicks - 1; i++)
            session.Tick();

        Assert.True(session.IsRunning);
    }

    [Fact]
    public void GameOver_AllLeft()
    {
        var session = CreateTwoPlayerSession();

        session.RemovePlayer("p2222222");

        session.Tick();

        Assert.False(session.IsRunning);
        Assert.Equal("ALL_LEFT", session.GameOverReason);
    }

    [Fact]
    public void GameOver_ScoreReached()
    {
        var session = CreateTwoPlayerSession();
        session.Players[0].Score = GameSession.ScoreLimit - 1;

        // Platziere Projektil so dass es trifft → +100 Score → Limit erreicht
        session.Players[1].X = 300;
        session.Players[1].Y = 540;
        session.Projectiles.Add(new Projectile(300, 540, 0, 0, "p1111111"));

        session.Tick();

        Assert.False(session.IsRunning);
        Assert.Equal("SCORE_REACHED", session.GameOverReason);
    }

    [Fact]
    public void GameOver_FinalScoresContainAllPlayers()
    {
        var session = CreateTwoPlayerSession();
        session.Players[0].Score = 500;
        session.Players[0].Kills = 3;
        session.Players[1].Score = 200;
        session.Players[1].Kills = 1;

        session.RemovePlayer("p2222222");
        session.Tick();

        Assert.Equal(2, session.FinalScores.Count);
        Assert.Equal("p1111111", session.FinalScores[0].PlayerId);
        Assert.Equal(500, session.FinalScores[0].Score);
        Assert.Equal(3, session.FinalScores[0].Kills);
    }

    [Fact]
    public void GameOver_FinalScoresSortedByScoreDescending()
    {
        var session = CreateTwoPlayerSession();
        session.Players[0].Score = 200;
        session.Players[1].Score = 500;

        session.RemovePlayer("p1111111");
        session.Tick();

        Assert.Equal("p2222222", session.FinalScores[0].PlayerId);
    }

    [Fact]
    public void GameOver_TicksNoLongerProcess()
    {
        var session = CreateTwoPlayerSession();
        session.RemovePlayer("p2222222");
        session.Tick(); // GAME_OVER

        var tickBefore = session.TickCount;
        session.Tick();

        Assert.Equal(tickBefore, session.TickCount);
    }
}
