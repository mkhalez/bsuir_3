using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using labs.Domain.Entities;

namespace labs.Data
{
    public class TempDbContext : DbContext
    {
        public TempDbContext (DbContextOptions<TempDbContext> options)
            : base(options)
        {
        }

        public DbSet<labs.Domain.Entities.Dish> Dish { get; set; } = default!;
    }
}
