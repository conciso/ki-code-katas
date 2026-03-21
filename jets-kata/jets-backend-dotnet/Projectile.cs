namespace jets_backend_dotnet;

public class Projectile
{
    private static int _nextId;

    public string Id { get; }
    public double X { get; set; }
    public double Y { get; set; }
    public double Vx { get; }
    public double Vy { get; }
    public string Owner { get; }

    public Projectile(double x, double y, double vx, double vy, string owner)
    {
        Id = $"b{Interlocked.Increment(ref _nextId):D3}";
        X = x;
        Y = y;
        Vx = vx;
        Vy = vy;
        Owner = owner;
    }
}
