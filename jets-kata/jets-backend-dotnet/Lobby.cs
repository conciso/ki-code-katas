namespace jets_backend_dotnet;

public class LobbyPlayer
{
    public string Id { get; }
    public string Name { get; }
    public bool Ready { get; set; }
    public string Color { get; }

    public LobbyPlayer(string id, string name, string color)
    {
        Id = id;
        Name = name;
        Color = color;
    }
}

public class Lobby
{
    private static readonly string[] PlayerColors = ["#FF4444", "#4444FF", "#44FF44", "#FFFF44"];

    public string Code { get; }
    public string HostId { get; private set; }
    public string GameMode { get; set; } = "FFA";
    public List<LobbyPlayer> Players { get; } = [];

    public bool IsFull => Players.Count >= 4;

    public Lobby(Player host)
    {
        Code = GenerateLobbyCode();
        HostId = host.Id;
        AddPlayer(host);
    }

    public void AddPlayer(Player player)
    {
        var color = PlayerColors[Players.Count];
        Players.Add(new LobbyPlayer(player.Id, player.Name, color));
    }

    public void RemovePlayer(string playerId)
    {
        Players.RemoveAll(p => p.Id == playerId);

        if (HostId == playerId && Players.Count > 0)
            HostId = Players[0].Id;
    }

    private static string GenerateLobbyCode()
    {
        const string chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        return new string(Enumerable.Range(0, 6)
            .Select(_ => chars[Random.Shared.Next(chars.Length)])
            .ToArray());
    }
}
