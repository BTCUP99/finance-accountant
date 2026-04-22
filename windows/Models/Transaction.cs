using System;
using System.ComponentModel.DataAnnotations;

namespace FinanceAccountant.Models;

public class Transaction
{
    [Key]
    public int Id { get; set; }

    public TransactionType Type { get; set; }

    public string Category { get; set; } = string.Empty;

    public decimal Amount { get; set; }

    public DateTime Date { get; set; }

    public string? Note { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.Now;
}

public enum TransactionType
{
    Income,  // 收入
    Expense  // 支出
}