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
