using labs.Controllers;
using labs.Domain.Entities;
using labs.Domain.Models;
using labs.Services.CategoryService;
using labs.Services.ProductService;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.ViewFeatures;
using Microsoft.Extensions.DependencyInjection;
using NSubstitute;

namespace labs.Tests;

public class ProductControllerTests
{
    private static readonly List<Category> Categories = new()
    {
        new Category { Id = 1, Name = "Супы", NormalizedName = "soups" },
        new Category { Id = 2, Name = "Салаты", NormalizedName = "salads" },
    };

    private static ProductController CreateController(
        IProductService productService,
        ICategoryService categoryService)
    {
        var tempDataFactory = Substitute.For<ITempDataDictionaryFactory>();
        tempDataFactory.GetTempData(Arg.Any<HttpContext>())
            .Returns(Substitute.For<ITempDataDictionary>());

        var services = new ServiceCollection();
        services.AddSingleton<ITempDataDictionaryFactory>(tempDataFactory);

        var httpContext = new DefaultHttpContext
        {
            RequestServices = services.BuildServiceProvider()
        };

        return new ProductController(productService, categoryService)
        {
            ControllerContext = new ControllerContext { HttpContext = httpContext }
        };
    }

    private static ICategoryService CategoryServiceWith(List<Category> categories)
    {
        var categoryService = Substitute.For<ICategoryService>();
        categoryService.GetCategoryListAsync()
            .Returns(ResponseData<List<Category>>.Success(categories));
        return categoryService;
    }

    private static IProductService ProductServiceWith(ResponseData<ListModel<Dish>> response)
    {
        var productService = Substitute.For<IProductService>();
        productService.GetProductListAsync(Arg.Any<string?>(), Arg.Any<int>())
            .Returns(response);
        return productService;
    }

    [Fact]
    public async Task Index_ReturnsNotFound_WhenCategoriesRequestFails()
    {
        var categoryService = Substitute.For<ICategoryService>();
        categoryService.GetCategoryListAsync()
            .Returns(ResponseData<List<Category>>.Error("Категории недоступны"));
        var controller = CreateController(Substitute.For<IProductService>(), categoryService);

        var result = await controller.Index(null);

        Assert.IsType<NotFoundObjectResult>(result);
    }

    [Fact]
    public async Task Index_ReturnsNotFound_WhenProductsRequestFails()
    {
        var categoryService = CategoryServiceWith(Categories);
        var productService = ProductServiceWith(
            ResponseData<ListModel<Dish>>.Error("Блюда недоступны"));
        var controller = CreateController(productService, categoryService);

        var result = await controller.Index(null);

        Assert.IsType<NotFoundObjectResult>(result);
    }

    [Fact]
    public async Task Index_PutsCategoriesAndModelIntoView()
    {
        var expectedModel = new ListModel<Dish>
        {
            Items = new List<Dish> { new() { Id = 1, Name = "Борщ", Calories = 330 } },
            CurrentPage = 1,
            TotalPages = 1
        };
        var categoryService = CategoryServiceWith(Categories);
        var productService = ProductServiceWith(
            ResponseData<ListModel<Dish>>.Success(expectedModel));
        var controller = CreateController(productService, categoryService);

        var result = await controller.Index(null);

        var viewResult = Assert.IsType<ViewResult>(result);
        var viewCategories = Assert.IsType<List<Category>>(viewResult.ViewData["categories"]);
        Assert.Equal(Categories, viewCategories);
        Assert.Same(expectedModel, viewResult.Model);
    }

    [Fact]
    public async Task Index_SetsCurrentCategoryToAll_WhenCategoryNotPassed()
    {
        var categoryService = CategoryServiceWith(Categories);
        var productService = ProductServiceWith(
            ResponseData<ListModel<Dish>>.Success(new ListModel<Dish>()));
        var controller = CreateController(productService, categoryService);

        var result = await controller.Index(null);

        var viewResult = Assert.IsType<ViewResult>(result);
        Assert.Equal("Все", viewResult.ViewData["currentCategory"]);
    }

    [Fact]
    public async Task Index_SetsCurrentCategoryName_WhenCategoryPassed()
    {
        var categoryService = CategoryServiceWith(Categories);
        var productService = ProductServiceWith(
            ResponseData<ListModel<Dish>>.Success(new ListModel<Dish>()));
        var controller = CreateController(productService, categoryService);

        var result = await controller.Index("soups");

        var viewResult = Assert.IsType<ViewResult>(result);
        Assert.Equal("Супы", viewResult.ViewData["currentCategory"]);
    }
}
