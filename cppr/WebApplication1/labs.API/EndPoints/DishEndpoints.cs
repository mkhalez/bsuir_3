using System.Text.Json;
using labs.API.Data;
using labs.API.Use_Cases;
using labs.Domain.Entities;
using MediatR;
using Microsoft.AspNetCore.Http.HttpResults;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace labs.API.EndPoints;

public static class DishEndpoints
{
    public static void MapDishEndpoints(this IEndpointRouteBuilder routes)
    {
        var group = routes.MapGroup("/api/Dish")
            .DisableAntiforgery();
        
        group.MapGet("/{category:alpha?}", async (
            IMediator mediator,
            string? category,
            int pageNo = 1,
            int pageSize = 3) =>
        {
            var data = await mediator.Send(
                new GetListOfProducts(category, pageNo, pageSize));
            return TypedResults.Ok(data);
        })
        .WithName("GetAllDishes");

        group.MapGet("/{id:int}", async Task<Results<Ok<Dish>, NotFound>> (int id, AppDbContext db) =>
        {
            return await db.Dishes.AsNoTracking()
                .Include(model => model.Category)
                .FirstOrDefaultAsync(model => model.Id == id)
                is Dish model
                    ? TypedResults.Ok(model)
                    : TypedResults.NotFound();
        })
        .WithName("GetDishById");

        group.MapPut("/{id:int}", async Task<IResult> (
            int id,
            [FromForm] string dish,
            [FromForm] IFormFile? file,
            AppDbContext db,
            IMediator mediator,
            IWebHostEnvironment env) =>
        {
            var existing = await db.Dishes.FindAsync(id);
            if (existing is null)
                return TypedResults.NotFound();

            var updated = JsonSerializer.Deserialize<Dish>(dish);
            if (updated is null)
                return TypedResults.BadRequest("Некорректные данные блюда");
            
            if (file is not null)
            {
                DeleteImageFile(existing.Image, env);
                existing.Image = await mediator.Send(new SaveImage(file));
            }

            existing.Name = updated.Name;
            existing.Description = updated.Description;
            existing.Calories = updated.Calories;
            existing.MimeType = updated.MimeType;
            existing.CategoryId = updated.CategoryId;

            await db.SaveChangesAsync();
            return TypedResults.Ok();
        })
        .WithName("UpdateDish");

        group.MapPost("/", async (
            [FromForm] string dish,
            [FromForm] IFormFile? file,
            AppDbContext db,
            IMediator mediator) =>
        {
            var newDish = JsonSerializer.Deserialize<Dish>(dish);
            if (newDish is null)
            {
                return Results.BadRequest("Некорректные данные блюда");
            }

            if (file is not null)
            {
                newDish.Image = await mediator.Send(new SaveImage(file));
            }

            db.Dishes.Add(newDish);
            await db.SaveChangesAsync();
            return TypedResults.Created($"/api/Dish/{newDish.Id}", newDish);
        })
        .WithName("CreateDish");

        group.MapDelete("/{id:int}", async Task<Results<Ok, NotFound>> (int id, AppDbContext db, IWebHostEnvironment env) =>
        {
            var existing = await db.Dishes.FindAsync(id);
            if (existing is null)
                return TypedResults.NotFound();
            
            DeleteImageFile(existing.Image, env);

            db.Dishes.Remove(existing);
            await db.SaveChangesAsync();
            return TypedResults.Ok();
        })
        .WithName("DeleteDish");
    }
    
    private static void DeleteImageFile(string? imageUrl, IWebHostEnvironment env)
    {
        if (string.IsNullOrWhiteSpace(imageUrl))
            return;
        try
        {
            var path = imageUrl.Contains("://")
                ? new Uri(imageUrl).LocalPath
                : imageUrl.Replace('/', Path.DirectorySeparatorChar);
            var fileName = Path.GetFileName(path);
            if (string.IsNullOrEmpty(fileName))
                return;
            var imagesFolder = Path.Combine(env.WebRootPath, "Images");
            var fullPath = Path.GetFullPath(Path.Combine(imagesFolder, fileName));
            // защита от выхода за пределы папки Images
            if (!fullPath.StartsWith(Path.GetFullPath(imagesFolder)))
                return;
            if (File.Exists(fullPath))
                File.Delete(fullPath);
        }
        catch
        {
            
        }
    }
}