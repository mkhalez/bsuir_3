namespace labs.Domain.Entities;

public class Dish
{
    public int Id { get; set; }
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    
    public int Calories { get; set; } 
    
    public string? Image { get; set; } 
    
    public string? MimeType { get; set; }
    
    public int CategoryId { get; set; }
    
    public Category? Category { get; set; }
}