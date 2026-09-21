using labs.Domain.Entities; 
using Microsoft.EntityFrameworkCore;

namespace labs.API.Data;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options)
    {
    }

    public DbSet<Dish> Dishes { get; set; }
    public DbSet<Category> Categories { get; set; }
}