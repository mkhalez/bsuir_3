using labs.Domain.Entities;

namespace labs.Domain.Models;

public class Cart
{
    public Dictionary<int, CartItem> CartItems { get; set; } = new();

    public virtual void AddToCart(Dish dish)
    {
        if (CartItems.TryGetValue(dish.Id, out var item))
            item.Count++;
        else
            CartItems[dish.Id] = new CartItem { Item = dish, Count = 1 };
    }

    public virtual void RemoveItems(int id)
    {
        CartItems.Remove(id);
    }

    public virtual void ClearAll()
    {
        CartItems.Clear();
    }

    public int Count => CartItems.Values.Sum(item => item.Count);

    public double TotalCalories =>
        CartItems.Values.Sum(item => (double)item.Item.Calories * item.Count);
}
