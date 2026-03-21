namespace jets_backend_dotnet;

public class PlayerInput
{
    public bool Left { get; set; }
    public bool Right { get; set; }
    public bool Up { get; set; }
    public bool Down { get; set; }
    public bool Shoot { get; set; }
    public int Seq { get; set; }
}

public class PlayerState
{
    public string Id { get; }
    public string Name { get; }
    public string Color { get; }
    public double X { get; set; }
    public double Y { get; set; }
    public double Angle { get; set; }
    public double Speed { get; set; }
    public int HP { get; set; }
    public int Score { get; set; }
    public bool Alive { get; set; }
    public int RespawnIn { get; set; }
    public bool Invincible { get; set; }
    public int InvincibleTicks { get; set; }
    public int LastProcessedInput { get; set; }
    public int ShootCooldown { get; set; }
    public int Kills { get; set; }
    public PlayerInput Input { get; set; } = new();

    public PlayerState(string id, string name, string color, double x, double y, double angle)
    {
        Id = id;
        Name = name;
        Color = color;
        X = x;
        Y = y;
        Angle = angle;
        Speed = GameSession.DefaultSpeed;
        HP = 3;
        Alive = true;
    }
}
