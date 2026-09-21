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

        if (await context.Categories.AnyAsync())
        {
            return;
        }

        var apiUrl = app.Configuration["ApiUrl"] ?? "https://localhost:7002";

        var categories = new List<Category>
        {
            new() { Name = "Супы",          NormalizedName = "soups" },
            new() { Name = "Основные блюда", NormalizedName = "maindishes" },
            new() { Name = "Салаты",        NormalizedName = "salads" },
            new() { Name = "Напитки",       NormalizedName = "drinks" }
        };

        await context.Categories.AddRangeAsync(categories);
        await context.SaveChangesAsync();

        int Cat(string normalizedName) =>
            categories.First(c => c.NormalizedName == normalizedName).Id;

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
                CategoryId = Cat("maindishes")
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