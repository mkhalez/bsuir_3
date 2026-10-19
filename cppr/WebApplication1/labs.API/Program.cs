using labs.API.Data;
using labs.API.EndPoints;
using Microsoft.EntityFrameworkCore;
using labs.API.Models;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.Extensions.DependencyInjection;

var builder = WebApplication.CreateBuilder(args);

var connectionString = builder.Configuration.GetConnectionString("Postgres");
builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseNpgsql(connectionString));

builder.Services.AddMediatR(cfg =>
    cfg.RegisterServicesFromAssembly(typeof(Program).Assembly));

builder.Services.AddHybridCache();
builder.Services.AddStackExchangeRedisCache(opt =>
{
    opt.InstanceName = "labs_";
    opt.Configuration = builder.Configuration.GetConnectionString("Redis");
});

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();
builder.Services.AddControllers();

builder.Services.AddHttpContextAccessor();


var authServer = builder.Configuration
    .GetSection("AuthServer")
    .Get<AuthServerData>()!;

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(JwtBearerDefaults.AuthenticationScheme, o =>
    {
        o.MetadataAddress = $"{authServer.Host}/realms/{authServer.Realm}/.well-known/openid-configuration";
        o.Authority = $"{authServer.Host}/realms/{authServer.Realm}";
        o.Audience = "account";
        o.RequireHttpsMetadata = false;
        o.Events = new JwtBearerEvents
        {
            OnAuthenticationFailed = ctx =>
            {
                Console.WriteLine($"JWT FAILED: {ctx.Exception.GetType().Name}: {ctx.Exception.Message}");
                return Task.CompletedTask;
            },
            OnChallenge = ctx =>
            {
                Console.WriteLine($"JWT CHALLENGE: {ctx.Error} | {ctx.ErrorDescription} | {ctx.AuthenticateFailure?.Message}");
                return Task.CompletedTask;
            }
        };
    });

builder.Services.AddAuthorization(opt =>
{
    opt.AddPolicy("admin", p => p.RequireRole("POWER-USER"));
});

var app = builder.Build();

app.UseAuthentication();
app.UseAuthorization();

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