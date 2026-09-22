using labs.API.Data;
using labs.API.EndPoints;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using labs.Data;

var builder = WebApplication.CreateBuilder(args);
builder.Services.AddDbContext<TempDbContext>(options =>
    options.UseSqlServer(builder.Configuration.GetConnectionString("TempDbContext") ?? throw new InvalidOperationException("Connection string 'TempDbContext' not found.")));

var connectionString = builder.Configuration.GetConnectionString("Postgres");
builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseNpgsql(connectionString));

// MediatR: ищет обработчики во всей сборке API
builder.Services.AddMediatR(cfg =>
    cfg.RegisterServicesFromAssembly(typeof(Program).Assembly));

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();
builder.Services.AddControllers();

builder.Services.AddHttpContextAccessor();

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();
app.UseStaticFiles();

await DbInitializer.SeedData(app);

app.MapDishEndpoints();
app.MapControllers();

app.Run();