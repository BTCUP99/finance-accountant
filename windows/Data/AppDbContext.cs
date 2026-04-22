using Microsoft.EntityFrameworkCore;
using FinanceAccountant.Models;
using System;
using System.IO;

namespace FinanceAccountant.Data;

public class AppDbContext : DbContext
{
    public DbSet<Transaction> Transactions { get; set; }

    protected override void OnConfiguring(DbContextOptionsBuilder optionsBuilder)
    {
        var dbPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "FinanceAccountant",
            "data.db");

        var dir = Path.GetDirectoryName(dbPath);
        if (!string.IsNullOrEmpty(dir) && !Directory.Exists(dir))
            Directory.CreateDirectory(dir);

        optionsBuilder.UseSqlite($"Data Source={dbPath}");
    }

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Transaction>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.Property(e => e.Type).IsRequired();
            entity.Property(e => e.Category).IsRequired().HasMaxLength(50);
            entity.Property(e => e.Amount).IsRequired();
            entity.Property(e => e.Date).IsRequired();
        });
    }
}