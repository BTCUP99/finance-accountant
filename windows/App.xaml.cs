using System;
using System.Windows;
using Serilog;

namespace FinanceAccountant;

public partial class App : Application
{
    public App()
    {
        // Configure logging
        Log.Logger = new LoggerConfiguration()
            .MinimumLevel.Information()
            .WriteTo.File(
                System.IO.Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "FinanceAccountant",
                    "logs",
                    "app_.log"),
                rollingInterval: RollingInterval.Day,
                retainedFileCountLimit: 7)
            .CreateLogger();

        // Global exception handling
        AppDomain.CurrentDomain.UnhandledException += (s, e) =>
        {
            Log.Fatal(e.ExceptionObject as Exception, "Unhandled exception");
            MessageBox.Show($"发生严重错误: {e.ExceptionObject}", "错误", MessageBoxButton.OK, MessageBoxImage.Error);
        };

        DispatcherUnhandledException += (s, e) =>
        {
            Log.Error(e.Exception, "Dispatcher unhandled exception");
            MessageBox.Show($"发生错误: {e.Exception.Message}", "错误", MessageBoxButton.OK, MessageBoxImage.Warning);
            e.Handled = true;
        };
    }

    protected override void OnStartup(StartupEventArgs e)
    {
        Log.Information("Application starting");
        base.OnStartup(e);
    }

    protected override void OnExit(ExitEventArgs e)
    {
        Log.Information("Application exiting");
        Log.CloseAndFlush();
        base.OnExit(e);
    }
}