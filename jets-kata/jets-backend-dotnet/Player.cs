using System.Net.WebSockets;

namespace jets_backend_dotnet;

public class Player
{
    public string Id { get; }
    public string Name { get; }
    public WebSocket Socket { get; }

    public Player(string id, string name, WebSocket socket)
    {
        Id = id;
        Name = name;
        Socket = socket;
    }
}
