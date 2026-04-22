package com.finance.accountant.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.finance.accountant.ui.theme.*
import com.finance.accountant.domain.model.Transaction
import com.finance.accountant.domain.model.TransactionType

data class PieChartData(val label: String, val value: Double, val color: Color)

@Composable
fun PieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty() || data.all { it.value == 0.0 }) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("暂无数据", color = DarkTextSecondary)
        }
        return
    }

    val total = remember(data) { data.sumOf { it.value } }
    val colors = listOf(AccentRed, AccentBlue, AccentGreen, AccentYellow, AccentPurple, AccentLightGreen)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            var startAngle = -90f
            val radius = minOf(size.width, size.height) / 2 * 0.8f
            val center = Offset(size.width / 2, size.height / 2)

            data.forEachIndexed { index, item ->
                val sweepAngle = (item.value / total * 360f).toFloat()
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
                startAngle += sweepAngle
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        data.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(color = colors[index % colors.size])
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${item.label} (${String.format("%.1f", item.value / total * 100)}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkText
                )
            }
        }
    }
}