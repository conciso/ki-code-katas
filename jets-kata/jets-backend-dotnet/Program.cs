using System.Net.WebSockets;
using System.Text;
using System.Text.Json;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.

builder.Services.AddControllers();
// Learn more about configuring OpenAPI at https://aka.ms/aspnet/openapi
builder.Services.AddOpenApi();

var app = builder.Build();

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
}

app.UseWebSockets();
app.UseAuthorization();

app.MapControllers();

app.Map("/ws/game", async context =>
{
    using var ws = await context.WebSockets.AcceptWebSocketAsync();
    var buffer = new byte[1024];

    while (true)
    {
        var result = await ws.ReceiveAsync(buffer, CancellationToken.None);
        if (result.MessageType == WebSocketMessageType.Close)
            break;

        var message = Encoding.UTF8.GetString(buffer, 0, result.Count);
        var response = HandleMessage(message);

        var responseBytes = Encoding.UTF8.GetBytes(response);
        await ws.SendAsync(responseBytes, WebSocketMessageType.Text, true, CancellationToken.None);
    }
});

static string HandleMessage(string message)
{
    try
    {
        using var doc = JsonDocument.Parse(message);
        var type = doc.RootElement.GetProperty("type").GetString();

        if (type == "ping")
        {
            var timestamp = doc.RootElement.GetProperty("timestamp").GetInt64();
            return JsonSerializer.Serialize(new { type = "pong", timestamp });
        }
    }
    catch (Exception) { }

    return message;
}

app.Run();


public partial class Program { }
