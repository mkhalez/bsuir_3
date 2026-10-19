using labs.Domain.Entities;

namespace labs.Domain.Models;

public class CartItem
{
    public Dish Item { get; set; } = new();
    public int Count { get; set; }
}
