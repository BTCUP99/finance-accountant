using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using FinanceAccountant.Models;
using FinanceAccountant.Services;
using System;
using System.Collections.ObjectModel;
using System.Linq;
using System.Threading.Tasks;
using LiveChartsCore;
using LiveChartsCore.SkiaSharpView;
using LiveChartsCore.SkiaSharpView.Painting;
using SkiaSharp;

namespace FinanceAccountant.ViewModels;

public partial class MainViewModel : ObservableObject
{
    private readonly TransactionService _service = new();

    [ObservableProperty] private ObservableCollection<Transaction> _transactions = new();
    [ObservableProperty] private ObservableCollection<Transaction> _filteredTransactions = new();
    [ObservableProperty] private string _selectedType = "全部";
    [ObservableProperty] private int _selectedYear;
    [ObservableProperty] private int _selectedMonth;
    [ObservableProperty] private decimal _totalIncome;
    [ObservableProperty] private decimal _totalExpense;
    [ObservableProperty] private decimal _balance;

    // 新增账单
    [ObservableProperty] private string _newCategory = string.Empty;
    [ObservableProperty] private decimal _newAmount;
    [ObservableProperty] private DateTime _newDate = DateTime.Today;
    [ObservableProperty] private string? _newNote;
    [ObservableProperty] private bool _isIncome = true;

    // 图表
    [ObservableProperty] private ISeries[] _pieSeries = Array.Empty<ISeries>();
    [ObservableProperty] private ISeries[] _lineSeries = Array.Empty<ISeries>();
    [ObservableProperty] private Axis[] _xAxes = Array.Empty<Axis>();
    [ObservableProperty] private Axis[] _yAxes = Array.Empty<Axis>();

    public ObservableCollection<string> IncomeCategories { get; } = new()
    {
        "工资", "奖金", "投资", "兼职", "其他收入"
    };

    public ObservableCollection<string> ExpenseCategories { get; } = new()
    {
        "餐饮", "交通", "购物", "居住", "医疗", "娱乐", "教育", "其他支出"
    };

    public ObservableCollection<string> TypeFilter { get; } = new() { "全部", "收入", "支出" };

    public ObservableCollection<string> MonthList { get; } = new();

    public MainViewModel()
    {
        _selectedYear = DateTime.Now.Year;
        _selectedMonth = DateTime.Now.Month;
        UpdateMonthList();
        _ = LoadDataAsync();
    }

    private void UpdateMonthList()
    {
        MonthList.Clear();
        for (int y = _selectedYear - 2; y <= _selectedYear; y++)
        {
            for (int m = 1; m <= 12; m++)
            {
                if (y < _selectedYear || (y == _selectedYear && m <= _selectedMonth))
                    MonthList.Add($"{y}年{m}月");
            }
        }
    }

    public async Task LoadDataAsync()
    {
        var all = await _service.GetByMonthAsync(_selectedYear, _selectedMonth);
        Transactions = new ObservableCollection<Transaction>(all);
        ApplyFilter();
        CalculateTotals();
        await UpdateChartsAsync();
    }

    private void ApplyFilter()
    {
        var filtered = Transactions.AsEnumerable();

        if (SelectedType == "收入")
            filtered = filtered.Where(t => t.Type == TransactionType.Income);
        else if (SelectedType == "支出")
            filtered = filtered.Where(t => t.Type == TransactionType.Expense);

        FilteredTransactions = new ObservableCollection<Transaction>(filtered);
    }

    private void CalculateTotals()
    {
        TotalIncome = Transactions.Where(t => t.Type == TransactionType.Income).Sum(t => t.Amount);
        TotalExpense = Transactions.Where(t => t.Type == TransactionType.Expense).Sum(t => t.Amount);
        Balance = TotalIncome - TotalExpense;
    }

    partial void OnSelectedTypeChanged(string value) => ApplyFilter();
    partial void OnSelectedYearChanged(int value)
    {
        UpdateMonthList();
        _ = LoadDataAsync();
    }
    partial void OnSelectedMonthChanged(int value) => _ = LoadDataAsync();

    [RelayCommand]
    private async Task AddTransactionAsync(CancellationToken ct)
    {
        if (NewAmount <= 0 || string.IsNullOrWhiteSpace(NewCategory))
            return;

        var transaction = new Transaction
        {
            Type = IsIncome ? TransactionType.Income : TransactionType.Expense,
            Category = NewCategory,
            Amount = NewAmount,
            Date = NewDate,
            Note = NewNote
        };

        await _service.AddAsync(transaction);
        await LoadDataAsync();

        NewAmount = 0;
        NewCategory = string.Empty;
        NewNote = null;
    }

    [RelayCommand]
    private async Task DeleteTransactionAsync(Transaction t, CancellationToken ct)
    {
        await _service.DeleteAsync(t.Id);
        await LoadDataAsync();
    }

    [RelayCommand]
    private async Task ExportCsvAsync(CancellationToken ct)
    {
        var dialog = new Microsoft.Win32.SaveFileDialog
        {
            Filter = "CSV 文件|*.csv",
            FileName = $"账单_{_selectedYear}_{_selectedMonth}.csv"
        };

        if (dialog.ShowDialog() == true)
        {
            await _service.ExportToCsvAsync(dialog.FileName, Transactions.ToList());
        }
    }

    [RelayCommand]
    private async Task ExportExcelAsync(CancellationToken ct)
    {
        var dialog = new Microsoft.Win32.SaveFileDialog
        {
            Filter = "Excel 文件|*.xlsx",
            FileName = $"账单_{_selectedYear}_{_selectedMonth}.xlsx"
        };

        if (dialog.ShowDialog() == true)
        {
            await _service.ExportToExcelAsync(dialog.FileName, Transactions.ToList());
        }
    }

    private async Task UpdateChartsAsync()
    {
        var stats = await _service.GetMonthlyStatisticsAsync(_selectedYear, _selectedMonth);

        // 饼图 - 支出分类
        var expenseStats = stats
            .Where(s => s.Key.StartsWith("支出_"))
            .OrderByDescending(s => s.Value)
            .Take(6)
            .ToList();

        var pieColors = new SKColor[]
        {
            SKColor.Parse("#FF6B6B"), SKColor.Parse("#4ECDC4"), SKColor.Parse("#45B7D1"),
            SKColor.Parse("#96CEB4"), SKColor.Parse("#FFEAA7"), SKColor.Parse("#DDA0DD")
        };

        PieSeries = expenseStats.Select((s, i) => new PieSeries<decimal>
        {
            Values = new[] { s.Value },
            Name = s.Key.Replace("支出_", ""),
            Fill = new SolidColorPaint(pieColors[i % pieColors.Length]),
            DataLabelsPaint = new SolidColorPaint(SKColors.White),
            DataLabelsSize = 12
        } as ISeries).ToArray();

        // 折线图 - 每日收支趋势
        var dailyIncome = Transactions
            .Where(t => t.Type == TransactionType.Income)
            .GroupBy(t => t.Date.Date)
            .ToDictionary(g => g.Key, g => g.Sum(t => t.Amount));

        var dailyExpense = Transactions
            .Where(t => t.Type == TransactionType.Expense)
            .GroupBy(t => t.Date.Date)
            .ToDictionary(g => g.Key, g => g.Sum(t => t.Amount));

        var daysInMonth = Enumerable.Range(1, DateTime.DaysInMonth(_selectedYear, _selectedMonth)).ToList();
        var allDays = daysInMonth.Select(d => new DateTime(_selectedYear, _selectedMonth, d)).ToList();

        var incomeValues = allDays.Select(d => dailyIncome.GetValueOrDefault(d, 0)).ToArray();
        var expenseValues = allDays.Select(d => dailyExpense.GetValueOrDefault(d, 0)).ToArray();

        LineSeries = new ISeries[]
        {
            new LineSeries<decimal>
            {
                Values = incomeValues,
                Name = "收入",
                Stroke = new SolidColorPaint(SKColor.Parse("#4ECDC4")) { StrokeThickness = 2 },
                GeometryStroke = new SolidColorPaint(SKColor.Parse("#4ECDC4")) { StrokeThickness = 2 },
                GeometryFill = new SolidColorPaint(SKColors.White),
                GeometrySize = 8,
                Fill = null
            },
            new LineSeries<decimal>
            {
                Values = expenseValues,
                Name = "支出",
                Stroke = new SolidColorPaint(SKColor.Parse("#FF6B6B")) { StrokeThickness = 2 },
                GeometryStroke = new SolidColorPaint(SKColor.Parse("#FF6B6B")) { StrokeThickness = 2 },
                GeometryFill = new SolidColorPaint(SKColors.White),
                GeometrySize = 8,
                Fill = null
            }
        };

        XAxes = new Axis[]
        {
            new Axis
            {
                Labels = allDays.Select(d => d.Day.ToString()).ToArray(),
                LabelsPaint = new SolidColorPaint(SKColor.Parse("#666666")),
                TextSize = 10
            }
        };

        YAxes = new Axis[]
        {
            new Axis
            {
                LabelsPaint = new SolidColorPaint(SKColor.Parse("#666666")),
                TextSize = 10,
                MinLimit = 0
            }
        };
    }
}