package com.finance.accountant.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.finance.accountant.ui.theme.*
import com.finance.accountant.domain.model.Transaction
import com.finance.accountant.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LineChart(
    transactions: List<Transaction>,
    selectedYear: Int,
    selectedMonth: Int,
    modifier: Modifier = Modifier
) {
    val daysInMonth = Calendar.getInstance().apply {
        set(selectedYear, selectedMonth - 1, 1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)

    val dailyIncome = mutableMapOf<Int, Double>()
    val dailyExpense = mutableMapOf<Int, Double>()

    transactions.forEach { t ->
        val calendar = Calendar.getInstance().apply { timeInMillis = t.date }
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        if (calendar.get(Calendar.YEAR) == selectedYear && calendar.get(Calendar.MONTH) == selectedMonth - 1) {
            if (t.type == TransactionType.INCOME) {
                dailyIncome[day] = (dailyIncome[day] ?: 0.0) + t.amount
            } else {
                dailyExpense[day] = (dailyExpense[day] ?: 0.0) + t.amount
            }
        }
    }

    val maxValue = maxOf(
        dailyIncome.values.maxOrNull() ?: 0.0,
        dailyExpense.values.maxOrNull() ?: 0.0,
        100.0
    )

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val width = size.width
            val height = size.height
            val stepX = width / (daysInMonth - 1).coerceAtLeast(1)

            // Draw grid lines
            for (i in 0..4) {
                val y = height - (height * i / 4)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            // Draw income line
            val incomePath = Path()
            var firstIncome = true
            dailyIncome.entries.sortedBy { it.key }.forEach { (day, value) ->
                val x = stepX * (day - 1)
                val y = height - (value / maxValue * height).toFloat()
                if (firstIncome) {
                    incomePath.moveTo(x, y)
                    firstIncome = false
                } else {
                    incomePath.lineTo(x, y)
                }
            }
            drawPath(
                path = incomePath,
                color = AccentGreen,
                style = Stroke(width = 3f)
            )

            // Draw expense line
            val expensePath = Path()
            var firstExpense = true
            dailyExpense.entries.sortedBy { it.key }.forEach { (day, value) ->
                val x = stepX * (day - 1)
                val y = height - (value / maxValue * height).toFloat()
                if (firstExpense) {
                    expensePath.moveTo(x, y)
                    firstExpense = false
                } else {
                    expensePath.lineTo(x, y)
                }
            }
            drawPath(
                path = expensePath,
                color = AccentRed,
                style = Stroke(width = 3f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(color = AccentGreen)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("收入", style = MaterialTheme.typography.bodySmall, color = DarkText)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(color = AccentRed)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("支出", style = MaterialTheme.typography.bodySmall, color = DarkText)
            }
        }
    }
}