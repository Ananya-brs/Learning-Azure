using Microsoft.AspNetCore.Mvc.RazorPages;

namespace SimpleWebApp.Pages;

public class IndexModel : PageModel
{
    private readonly IHostEnvironment _environment;
    private readonly ILogger<IndexModel> _logger;

    public IndexModel(IHostEnvironment environment, ILogger<IndexModel> logger)
    {
        _environment = environment;
        _logger = logger;
    }

    public string EnvironmentName { get; private set; } = string.Empty;

    public void OnGet()
    {
        EnvironmentName = _environment.EnvironmentName;
        _logger.LogInformation("Home page loaded in {Environment} on {Machine}.", EnvironmentName, Environment.MachineName);
    }
}
