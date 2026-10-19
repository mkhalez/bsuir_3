using labs.API.Data;
using labs.API.Use_Cases;
using labs.Domain.Entities;
using labs.Domain.Models;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;

namespace labs.Tests;

public class GetListOfProductsHandlerTests : IDisposable
{
    private readonly SqliteConnection _connection;
    private readonly AppDbContext _context;
    private readonly GetListOfProductsHandler _handler;

    public GetListOfProductsHandlerTests()
    {
        _connection = new SqliteConnection("DataSource=:memory:");
        _connection.Open();

        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseSqlite(_connection)
            .Options;

        _context = new AppDbContext(options);
        _context.Database.EnsureCreated();

        Seed();

        _handler = new GetListOfProductsHandler(_context);
    }

    private void Seed()
    {
        var soups = new Category { Name = "Супы", NormalizedName = "soups" };
        var salads = new Category { Name = "Салаты", NormalizedName = "salads" };
        var drinks = new Category { Name = "Напитки", NormalizedName = "drinks" };

        _context.Categories.AddRange(soups, salads, drinks);
        _context.SaveChanges();

        var dishes = new List<Dish>();
        for (var i = 0; i < 10; i++)
        {
            dishes.Add(new Dish
            {
                Name = $"Суп {i}", Calories = 100, CategoryId = soups.Id
            });
        }

        for (var i = 0; i < 8; i++)
        {
            dishes.Add(new Dish
            {
                Name = $"Салат {i}", Calories = 200, CategoryId = salads.Id
            });
        }

        for (var i = 0; i < 7; i++)
        {
            dishes.Add(new Dish
            {
                Name = $"Напиток {i}", Calories = 50, CategoryId = drinks.Id
            });
        }

        _context.Dishes.AddRange(dishes);
        _context.SaveChanges();
    }

    [Fact]
    public async Task ReturnsFirstPageOfThreeItemsByDefault()
    {
        var result = await _handler.Handle(new GetListOfProducts(null), CancellationToken.None);

        Assert.IsType<ResponseData<ListModel<Dish>>>(result);
        Assert.True(result.Successfull);
        Assert.Equal(1, result.Data!.CurrentPage);
        Assert.Equal(3, result.Data.Items.Count);
        Assert.Equal(9, result.Data.TotalPages);
        Assert.Equal(_context.Dishes.OrderBy(d => d.Id).First().Id, result.Data.Items[0].Id);
    }

    [Fact]
    public async Task SelectsRequestedPage()
    {
        var expectedIds = _context.Dishes
            .OrderBy(d => d.Id)
            .Skip(3)
            .Take(3)
            .Select(d => d.Id)
            .ToList();

        var result = await _handler.Handle(
            new GetListOfProducts(null, PageNo: 2), CancellationToken.None);

        Assert.True(result.Successfull);
        Assert.Equal(2, result.Data!.CurrentPage);
        Assert.Equal(expectedIds, result.Data.Items.Select(d => d.Id).ToList());
    }

    [Fact]
    public async Task FiltersItemsByCategory()
    {
        var result = await _handler.Handle(
            new GetListOfProducts("soups", PageNo: 1, PageSize: 20), CancellationToken.None);

        Assert.True(result.Successfull);
        Assert.Equal(10, result.Data!.Items.Count);
        Assert.All(result.Data.Items, d => Assert.StartsWith("Суп", d.Name));
        Assert.Equal(1, result.Data.TotalPages);
    }

    [Fact]
    public async Task DoesNotAllowPageSizeGreaterThanMaximum()
    {
        var result = await _handler.Handle(
            new GetListOfProducts(null, PageNo: 1, PageSize: 100), CancellationToken.None);

        Assert.True(result.Successfull);
        Assert.Equal(20, result.Data!.Items.Count);
        Assert.Equal(2, result.Data.TotalPages);
    }

    [Fact]
    public async Task ReturnsError_WhenPageNumberExceedsTotalPages()
    {
        var result = await _handler.Handle(
            new GetListOfProducts(null, PageNo: 999), CancellationToken.None);

        Assert.False(result.Successfull);
        Assert.NotNull(result.ErrorMessage);
    }

    public void Dispose()
    {
        _context.Dispose();
        _connection.Dispose();
    }
}
