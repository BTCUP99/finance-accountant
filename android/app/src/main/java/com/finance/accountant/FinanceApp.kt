package com.finance.accountant

import android.app.Application
import com.finance.accountant.data.local.AppDatabase

class FinanceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize database
        AppDatabase.getInstance(this)
    }
}