using labs.API.Data;
using labs.API.Use_Cases;
using labs.Domain.Entities;
using MediatR;
using Microsoft.AspNetCore.Http.HttpResults;
using Microsoft.EntityFrameworkCore;

namespace labs.API.EndPoints;

public static class DishEndpoints
{
    public static void MapDishEndpoints(this IEndpointRouteBuilder routes)
    {
        var group = routes.MapGroup("/api/Dish");

        // список с фильтром по категории и пагинацией
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
                .FirstOrDefaultAsync(model => model.Id == id)
                is Dish model
                    ? TypedResults.Ok(model)
                    : TypedResults.NotFound();
        })
        .WithName("GetDishById");

        group.MapPut("/{id:int}", async Task<Results<Ok, NotFound>> (int id, Dish dish, AppDbContext db) =>
        {
            var affected = await db.Dishes
                .Where(model => model.Id == id)
                .ExecuteUpdateAsync(setters => setters
                    .SetProperty(m => m.Name, dish.Name)
                    .SetProperty(m => m.Description, dish.Description)
                    .SetProperty(m => m.Calories, dish.Calories)
                    .SetProperty(m => m.Image, dish.Image)
                    .SetProperty(m => m.MimeType, dish.MimeType)
                    .SetProperty(m => m.CategoryId, dish.CategoryId));
            return affected == 1 ? TypedResults.Ok() : TypedResults.NotFound();
        })
        .WithName("UpdateDish");

        group.MapPost("/", async (Dish dish, AppDbContext db) =>
        {
            db.Dishes.Add(dish);
            await db.SaveChangesAsync();
            return TypedResults.Created($"/api/Dish/{dish.Id}", dish);
        })
        .WithName("CreateDish");

        group.MapDelete("/{id:int}", async Task<Results<Ok, NotFound>> (int id, AppDbContext db) =>
        {
            var affected = await db.Dishes
                .Where(model => model.Id == id)
                .ExecuteDeleteAsync();
            return affected == 1 ? TypedResults.Ok() : TypedResults.NotFound();
        })
        .WithName("DeleteDish");
    }
}