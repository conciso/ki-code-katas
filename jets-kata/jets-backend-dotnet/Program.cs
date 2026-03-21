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

app.Map("/ws", async context =>
{
    using var ws = await context.WebSockets.AcceptWebSocketAsync();
    var buffer = new byte[1024];

    while (true)
    {
        var result = await ws.ReceiveAsync(buffer, CancellationToken.None);
        if (result.MessageType == System.Net.WebSockets.WebSocketMessageType.Close)
            break;

        await ws.SendAsync(
            buffer.AsMemory(0, result.Count),
            result.MessageType,
            result.EndOfMessage,
            CancellationToken.None);
    }
});

app.Run();

public partial class Program { }
