namespace jets_backend_dotnet;

public class FinalScore
{
    public string PlayerId { get; }
    public string Name { get; }
    public int Score { get; }
    public int Kills { get; }

    public FinalScore(string playerId, string name, int score, int kills)
    {
        PlayerId = playerId;
        Name = name;
        Score = score;
        Kills = kills;
    }
}
