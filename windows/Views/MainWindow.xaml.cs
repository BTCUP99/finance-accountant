using System;
using System.Windows;
using System.Windows.Controls;
using FinanceAccountant.ViewModels;

namespace FinanceAccountant.Views;

public partial class MainWindow : Window
{
    public MainViewModel ViewModel { get; }

    public MainWindow()
    {
        InitializeComponent();
        ViewModel = new MainViewModel();
        DataContext = ViewModel;

        // Initialize month combo
        UpdateMonthCombo();

        Loaded += MainWindow_Loaded;
    }

    private void MainWindow_Loaded(object sender, RoutedEventArgs e)
    {
        MonthCombo.SelectedIndex = MonthCombo.Items.Count - 1;
    }

    private void MonthCombo_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (MonthCombo.SelectedItem is string selected)
        {
            var parts = selected.Replace("年", "-").Replace("月", "").Split('-');
            if (parts.Length == 2 && int.TryParse(parts[0], out int year) && int.TryParse(parts[1], out int month))
            {
                ViewModel.SelectedYear = year;
                ViewModel.SelectedMonth = month;
            }
        }
    }

    private void TypeRadio_Changed(object sender, RoutedEventArgs e)
    {
        if (ViewModel == null) return;

        ViewModel.IsIncome = IncomeRadio.IsChecked == true;
        UpdateCategoryCombo();
    }

    private void UpdateCategoryCombo()
    {
        CategoryCombo.Items.Clear();

        if (ViewModel.IsIncome)
        {
            foreach (var cat in ViewModel.IncomeCategories)
                CategoryCombo.Items.Add(new ComboBoxItem { Content = cat });
        }
        else
        {
            foreach (var cat in ViewModel.ExpenseCategories)
                CategoryCombo.Items.Add(new ComboBoxItem { Content = cat });
        }

        if (CategoryCombo.Items.Count > 0)
            CategoryCombo.SelectedIndex = 0;
    }

    private void CategoryCombo_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (CategoryCombo.SelectedItem is ComboBoxItem item && item.Content is string cat)
        {
            ViewModel.NewCategory = cat;
        }
    }

    private void UpdateMonthCombo()
    {
        UpdateCategoryCombo();
    }
}