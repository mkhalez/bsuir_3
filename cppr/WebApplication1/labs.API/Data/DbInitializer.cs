using labs.Domain.Entities;
using Microsoft.EntityFrameworkCore;

namespace labs.API.Data;

public static class DbInitializer
{
    public static async Task SeedData(WebApplication app)
    {
        using var scope = app.Services.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<AppDbContext>();

        await context.Database.MigrateAsync();

        var apiUrl = app.Configuration["ApiUrl"] ?? "https://localhost:7002";
        
        var wrong = await context.Categories
            .FirstOrDefaultAsync(c => c.NormalizedName == "maindishes");
        if (wrong is not null)
        {
            wrong.NormalizedName = "main-dishes";
        }
        
        var required = new (string Name, string NormalizedName)[]
        {
            ("Супы", "soups"),
            ("Основные блюда", "main-dishes"),
            ("Салаты", "salads"),
            ("Напитки", "drinks"),
            ("Стартеры", "starters"),
            ("Десерты", "desserts"),
        };
        foreach (var (name, normalizedName) in required)
        {
            if (!await context.Categories.AnyAsync(c => c.NormalizedName == normalizedName))
            {
                context.Categories.Add(new Category { Name = name, NormalizedName = normalizedName });
            }
        }
        await context.SaveChangesAsync();

        if (await context.Dishes.AnyAsync())
        {
            return;
        }

        int Cat(string normalizedName) =>
            context.Categories.First(c => c.NormalizedName == normalizedName).Id;

        var dishes = new List<Dish>
        {
            new()
            {
                Name = "Борщ",
                Description = "Традиционный свекольный суп с говядиной",
                Calories = 330,
                Image = $"{apiUrl}/Images/Борщ.jpg",
                MimeType = "image/jpeg",
                CategoryId = Cat("soups")
            },
            new()
            {
                Name = "Харчо",
                Description = "Острый грузинский суп с рисом и говядиной",
                Calories = 280,
                Image = $"{apiUrl}/Images/Харчо.jpg",
                MimeType = "image/jpeg",
                CategoryId = Cat("soups")
            },
            new()
            {
                Name = "Стейк",
                Description = "Говяжий стейк на гриле",
                Calories = 520,
                Image = $"{apiUrl}/Images/Стейк.jpg",
                MimeType = "image/jpeg",
                CategoryId = Cat("main-dishes")
            },
            new()
            {
                Name = "Цезарь",
                Description = "Салат с курицей, сухариками и соусом цезарь",
                Calories = 350,
                Image = $"{apiUrl}/Images/Цезарь.jpg",
                MimeType = "image/jpeg",
                CategoryId = Cat("salads")
            },
            new()
            {
                Name = "Лимонад",
                Description = "Домашний лимонад с мятой",
                Calories = 120,
                Image = $"{apiUrl}/Images/Лимонад.jpg",
                MimeType = "image/jpeg",
                CategoryId = Cat("drinks")
            }
        };

        await context.Dishes.AddRangeAsync(dishes);
        await context.SaveChangesAsync();
    }
}
