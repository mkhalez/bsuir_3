using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text;
using System.Text.Json;
using labs.Domain.Entities;
using labs.Domain.Models;
using Microsoft.AspNetCore.Components.WebAssembly.Authentication;

namespace WebApplication1.BlazorWasm.Services;

public class DataService : IDataService
{
    private readonly HttpClient _httpClient;
    private readonly IAccessTokenProvider _tokenProvider;
    private readonly string _pageSize;
    private readonly JsonSerializerOptions _serializerOptions = new()
    {
        PropertyNameCaseInsensitive = true
    };

    public DataService(
        HttpClient httpClient,
        IConfiguration configuration,
        IAccessTokenProvider tokenProvider)
    {
        _httpClient = httpClient;
        _tokenProvider = tokenProvider;
        _pageSize = configuration["ItemsPerPage"] ?? "3";
    }

    public event Action DataLoaded = delegate { };

    public List<Category> Categories { get; set; } = new();

    public List<Dish> Dishes { get; set; } = new();

    public bool Success { get; set; } = true;

    public string ErrorMessage { get; set; } = string.Empty;

    public int TotalPages { get; set; } = 1;

    public int CurrentPage { get; set; } = 1;

    public Category? SelectedCategory { get; set; }

    public async Task GetProductListAsync(int pageNo = 1)
    {
        var request = new HttpRequestMessage(HttpMethod.Get, BuildProductListUri(pageNo));

        if (!await AddAuthorizationHeaderAsync(request))
        {
            DataLoaded();
            return;
        }

        try
        {
            var response = await _httpClient.SendAsync(request);

            if (!response.IsSuccessStatusCode)
            {
                Success = false;
                ErrorMessage = $"Данные не получены от сервера. Error: {response.StatusCode}";
                DataLoaded();
                return;
            }

            var data = await response.Content
                .ReadFromJsonAsync<ResponseData<ListModel<Dish>>>(_serializerOptions);

            if (data is null || !data.Successfull || data.Data is null)
            {
                Success = false;
                ErrorMessage = data?.ErrorMessage ?? "Пустой ответ от сервера";
                DataLoaded();
                return;
            }

            Dishes = data.Data.Items;
            CurrentPage = data.Data.CurrentPage;
            TotalPages = data.Data.TotalPages;
            Success = true;
            ErrorMessage = string.Empty;
        }
        catch (Exception ex)
        {
            Success = false;
            ErrorMessage = $"API недоступен: {ex.Message}";
        }

        DataLoaded();
    }

    public async Task GetCategoryListAsync()
    {
        try
        {
            var response = await _httpClient.GetAsync("Categories");

            if (!response.IsSuccessStatusCode)
            {
                Success = false;
                ErrorMessage = $"Категории не получены. Error: {response.StatusCode}";
                DataLoaded();
                return;
            }

            Categories = await response.Content
                .ReadFromJsonAsync<List<Category>>(_serializerOptions) ?? new List<Category>();
            Success = true;
            ErrorMessage = string.Empty;
        }
        catch (Exception ex)
        {
            Success = false;
            ErrorMessage = $"API недоступен: {ex.Message}";
        }

        DataLoaded();
    }

    private string BuildProductListUri(int pageNo)
    {
        var route = new StringBuilder("Dish/");

        if (SelectedCategory is not null)
        {
            route.Append($"{SelectedCategory.NormalizedName}/");
        }

        var query = new List<string>();

        if (pageNo > 1)
        {
            query.Add($"pageNo={pageNo}");
        }

        if (_pageSize != "3")
        {
            query.Add($"pageSize={_pageSize}");
        }

        if (query.Count > 0)
        {
            route.Append('?').Append(string.Join('&', query));
        }

        return route.ToString();
    }

    private async Task<bool> AddAuthorizationHeaderAsync(HttpRequestMessage request)
    {
        var tokenRequest = await _tokenProvider.RequestAccessToken();

        if (tokenRequest.TryGetToken(out var token))
        {
            request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token.Value);
            return true;
        }

        Success = false;
        ErrorMessage = "Не удалось получить JWT-токен";
        return false;
    }
}
