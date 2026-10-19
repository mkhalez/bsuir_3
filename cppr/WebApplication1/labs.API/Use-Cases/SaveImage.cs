using MediatR;

namespace labs.API.Use_Cases;

public sealed record SaveImage(IFormFile File) : IRequest<string>;

public class SaveImageHandler(
    IWebHostEnvironment env,
    IHttpContextAccessor httpContextAccessor)
    : IRequestHandler<SaveImage, string>
{
    public async Task<string> Handle(SaveImage request, CancellationToken cancellationToken)
    {
        
        var extension = Path.GetExtension(request.File.FileName);
        var fileName = $"{Guid.NewGuid()}{extension}";

        
        var imagesFolder = Path.Combine(env.WebRootPath, "Images");
        if (!Directory.Exists(imagesFolder))
        {
            Directory.CreateDirectory(imagesFolder);
        }
        var filePath = Path.Combine(imagesFolder, fileName);

        
        await using (var stream = new FileStream(filePath, FileMode.Create))
        {
            await request.File.CopyToAsync(stream, cancellationToken);
        }

        
        var httpRequest = httpContextAccessor.HttpContext?.Request;
        var baseUrl = $"{httpRequest?.Scheme}://{httpRequest?.Host}";

        return $"{baseUrl}/Images/{fileName}";
    }
}