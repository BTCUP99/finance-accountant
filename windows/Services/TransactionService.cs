using Microsoft.EntityFrameworkCore;
using FinanceAccountant.Data;
using FinanceAccountant.Models;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using ClosedXML.Excel;
using System.IO;

namespace FinanceAccountant.Services;

public class TransactionService
{
    public async Task<List<Transaction>> GetAllAsync()
    {
        using var db = new AppDbContext();
        return await db.Transactions.OrderByDescending(t => t.Date).ToListAsync();
    }

    public async Task<List<Transaction>> GetByMonthAsync(int year, int month)
    {
        using var db = new AppDbContext();
        return await db.Transactions
            .Where(t => t.Date.Year == year && t.Date.Month == month)
            .OrderByDescending(t => t.Date)
            .ToListAsync();
    }

    public async Task AddAsync(Transaction transaction)
    {
        using var db = new AppDbContext();
        db.Transactions.Add(transaction);
        await db.SaveChangesAsync();
    }

    public async Task UpdateAsync(Transaction transaction)
    {
        using var db = new AppDbContext();
        db.Transactions.Update(transaction);
        await db.SaveChangesAsync();
    }

    public async Task DeleteAsync(int id)
    {
        using var db = new AppDbContext();
        var t = await db.Transactions.FindAsync(id);
        if (t != null)
        {
            db.Transactions.Remove(t);
            await db.SaveChangesAsync();
        }
    }

    public async Task<Dictionary<string, decimal>> GetMonthlyStatisticsAsync(int year, int month)
    {
        var transactions = await GetByMonthAsync(year, month);
        var stats = new Dictionary<string, decimal>();

        foreach (var t in transactions)
        {
            var key = t.Type == TransactionType.Income ? $"收入_{t.Category}" : $"支出_{t.Category}";
            if (!stats.ContainsKey(key))
                stats[key] = 0;
            stats[key] += t.Amount;
        }

        return stats;
    }

    public async Task ExportToCsvAsync(string filePath, List<Transaction> transactions)
    {
        var lines = new List<string> { "日期,类型,分类,金额,备注" };
        lines.AddRange(transactions.Select(t =>
            $"{t.Date:yyyy-MM-dd},{t.Type},{t.Category},{t.Amount},{t.Note ?? ""}"));

        await File.WriteAllLinesAsync(filePath, lines, System.Text.Encoding.UTF8);
    }

    public async Task ExportToExcelAsync(string filePath, List<Transaction> transactions)
    {
        await Task.Run(() =>
        {
            using var wb = new XLWorkbook();
            var ws = wb.Worksheets.Add("账单记录");

            ws.Cell(1, 1).Value = "日期";
            ws.Cell(1, 2).Value = "类型";
            ws.Cell(1, 3).Value = "分类";
            ws.Cell(1, 4).Value = "金额";
            ws.Cell(1, 5).Value = "备注";

            for (int i = 0; i < transactions.Count; i++)
            {
                var t = transactions[i];
                ws.Cell(i + 2, 1).Value = t.Date.ToString("yyyy-MM-dd");
                ws.Cell(i + 2, 2).Value = t.Type == TransactionType.Income ? "收入" : "支出";
                ws.Cell(i + 2, 3).Value = t.Category;
                ws.Cell(i + 2, 4).Value = (double)t.Amount;
                ws.Cell(i + 2, 5).Value = t.Note ?? "";
            }

            ws.Columns().AdjustToContents();
            wb.SaveAs(filePath);
        });
    }
}