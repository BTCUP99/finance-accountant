package com.finance.accountant.data.repository

import com.finance.accountant.data.local.TransactionDao
import com.finance.accountant.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TransactionRepository(private val dao: TransactionDao) {

    fun getAllTransactions(): Flow<List<Transaction>> = dao.getAllTransactions()

    fun getTransactionsByMonth(year: Int, month: Int): Flow<List<Transaction>> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        val startTime = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val endTime = calendar.timeInMillis

        return dao.getTransactionsByMonth(startTime, endTime)
    }

    suspend fun add(transaction: Transaction) {
        dao.insert(transaction)
    }

    suspend fun update(transaction: Transaction) {
        dao.update(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        dao.delete(transaction)
    }

    suspend fun deleteById(id: Int) {
        dao.deleteById(id)
    }
}