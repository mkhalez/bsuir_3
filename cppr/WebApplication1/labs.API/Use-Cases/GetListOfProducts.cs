using labs.API.Data;
using labs.Domain.Entities;
using labs.Domain.Models;
using MediatR;
using Microsoft.EntityFrameworkCore;

namespace labs.API.Use_Cases;

public sealed record GetListOfProducts(
    string? CategoryNormalizedName,
    int PageNo = 1,
    int PageSize = 3) : IRequest<ResponseData<ListModel<Dish>>>;

public class GetListOfProductsHandler(AppDbContext db)
    : IRequestHandler<GetListOfProducts, ResponseData<ListModel<Dish>>>
{
    private readonly int _maxPageSize = 20;

    public async Task<ResponseData<ListModel<Dish>>> Handle(
        GetListOfProducts request, CancellationToken cancellationToken)
    {
        var pageSize = Math.Clamp(request.PageSize, 1, _maxPageSize);

        IQueryable<Dish> query = db.Dishes.AsNoTracking();
        
        if (!string.IsNullOrEmpty(request.CategoryNormalizedName))
        {
            query = query.Where(d =>
                d.Category!.NormalizedName == request.CategoryNormalizedName);
        }
        
        query = query.OrderBy(d => d.Id);

        var count = await query.CountAsync(cancellationToken);
        var totalPages = (int)Math.Ceiling(count / (double)pageSize);
        var pageNo = Math.Clamp(request.PageNo, 1, Math.Max(totalPages, 1));

        var items = await query
            .Skip((pageNo - 1) * pageSize)
            .Take(pageSize)
            .ToListAsync(cancellationToken);

        return ResponseData<ListModel<Dish>>.Success(new ListModel<Dish>
        {
            Items = items,
            CurrentPage = pageNo,
            TotalPages = totalPages
        });
    }
}