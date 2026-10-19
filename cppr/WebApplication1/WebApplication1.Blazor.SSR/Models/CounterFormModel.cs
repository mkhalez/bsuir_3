using System.ComponentModel.DataAnnotations;

namespace WebApplication1.Blazor.SSR.Models;

public class CounterFormModel
{
    [Range(1, 10, ErrorMessage = "Значение должно быть целым числом от 1 до 10.")]
    public int Value { get; set; }
}
