using jets_backend_dotnet;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers();
builder.Services.AddOpenApi();
builder.Services.AddSingleton<GameManager>();

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
}

app.UseWebSockets();
app.UseAuthorization();
app.MapControllers();

app.MapGet("/health", () => Results.Ok(new { status = "healthy" }));

app.MapGet("/api/lobbies", (GameManager gm) => Results.Ok(gm.GetLobbies()));

app.MapGet("/dashboard", () => Results.Content("""
<!DOCTYPE html>
<html>
<head>
  <title>Jets Dashboard</title>
  <meta charset="utf-8">
  <style>
    body { font-family: system-ui, sans-serif; background: #1a1a2e; color: #eee; margin: 2rem; }
    h1 { color: #0f3460; }
    table { border-collapse: collapse; width: 100%; margin-top: 1rem; }
    th, td { border: 1px solid #333; padding: 0.5rem 1rem; text-align: left; }
    th { background: #16213e; }
    .waiting { color: #4ecca3; }
    .in_game { color: #fc5185; }
    .empty { color: #666; font-style: italic; padding: 2rem; text-align: center; }
  </style>
</head>
<body>
  <h1>Jets — Lobby Dashboard</h1>
  <div id="content"><p class="empty">Loading...</p></div>
  <script>
    async function refresh() {
      const res = await fetch('/api/lobbies');
      const lobbies = await res.json();
      const el = document.getElementById('content');
      if (lobbies.length === 0) {
        el.innerHTML = '<p class="empty">Keine aktiven Lobbies</p>';
        return;
      }
      let html = '<table><tr><th>Code</th><th>Status</th><th>Spieler</th><th>Modus</th></tr>';
      for (const l of lobbies) {
        const names = l.players.map(p => p.name + (p.ready ? ' ✓' : '')).join(', ');
        html += `<tr>
          <td><code>${l.code}</code></td>
          <td class="${l.status}">${l.status === 'in_game' ? 'Im Spiel' : 'Wartet'}</td>
          <td>${names} (${l.playerCount}/4)</td>
          <td>${l.gameMode}</td>
        </tr>`;
      }
      el.innerHTML = html + '</table>';
    }
    refresh();
    setInterval(refresh, 2000);
  </script>
</body>
</html>
""", "text/html"));

app.Map("/ws/game", async context =>
{
    if (!context.WebSockets.IsWebSocketRequest)
    {
        context.Response.StatusCode = 400;
        return;
    }

    var playerName = context.Request.Query["playerName"].ToString();
    using var ws = await context.WebSockets.AcceptWebSocketAsync();
    var gameManager = context.RequestServices.GetRequiredService<GameManager>();
    await gameManager.HandleConnectionAsync(ws, playerName);
});

app.Run();

public partial class Program { }
