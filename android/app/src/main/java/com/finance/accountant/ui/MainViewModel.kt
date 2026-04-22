package com.finance.accountant.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.finance.accountant.data.local.AppDatabase
import com.finance.accountant.data.repository.TransactionRepository
import com.finance.accountant.domain.model.Transaction
import com.finance.accountant.domain.model.TransactionType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class UiState(
    val transactions: List<Transaction> = emptyList(),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val isLoading: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)

    val incomeCategories = listOf("工资", "奖金", "投资", "兼职", "其他收入")
    val expenseCategories = listOf("餐饮", "交通", "购物", "居住", "医疗", "娱乐", "教育", "其他支出")

    init {
        val db = AppDatabase.getInstance(application)
        repository = TransactionRepository(db.transactionDao())

        viewModelScope.launch {
            combine(_selectedYear, _selectedMonth) { year, month ->
                Pair(year, month)
            }.collectLatest { (year, month) ->
                repository.getTransactionsByMonth(year, month).collect { list ->
                    val income = list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                    val expense = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

                    _uiState.update {
                        it.copy(
                            transactions = list,
                            selectedYear = year,
                            selectedMonth = month,
                            totalIncome = income,
                            totalExpense = expense,
                            balance = income - expense
                        )
                    }
                }
            }
        }
    }

    fun setMonth(year: Int, month: Int) {
        _selectedYear.value = year
        _selectedMonth.value = month
    }

    fun addTransaction(
        isIncome: Boolean,
        category: String,
        amount: Double,
        date: Long,
        note: String?
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
                category = category,
                amount = amount,
                date = date,
                note = note
            )
            repository.add(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteById(transaction.id)
        }
    }

    fun getExportData(): List<Transaction> = _uiState.value.transactions

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}