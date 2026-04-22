package com.finance.accountant.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.finance.accountant.domain.model.Transaction
import com.finance.accountant.domain.model.TransactionType
import com.finance.accountant.ui.MainViewModel
import com.finance.accountant.ui.components.*
import com.finance.accountant.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    // 生成饼图数据
    val pieData = remember(uiState.transactions) {
        val expenseTransactions = uiState.transactions.filter { it.type == TransactionType.EXPENSE }
        val categoryMap = expenseTransactions.groupBy { it.category }
            .mapValues { it.value.sumOf { t -> t.amount } }
            .entries
            .sortedByDescending { it.value }
            .take(6)

        val colors = listOf(AccentRed, AccentBlue, AccentGreen, AccentYellow, AccentPurple, AccentLightGreen)
        categoryMap.mapIndexed { index, entry ->
            PieChartData(entry.key, entry.value, colors[index % colors.size])
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AccentGreen,
                contentColor = DarkBackground
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                // 标题
                Text(
                    text = "财务记账",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen
                )

                // 月份选择
                Spacer(modifier = Modifier.height(8.dp))
                MonthSelector(
                    year = uiState.selectedYear,
                    month = uiState.selectedMonth,
                    onMonthChange = { year, month ->
                        viewModel.setMonth(year, month)
                    }
                )
            }

            // 统计卡片
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "收入",
                        value = String.format("¥%.2f", uiState.totalIncome),
                        color = AccentGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "支出",
                        value = String.format("¥%.2f", uiState.totalExpense),
                        color = AccentRed,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "结余",
                        value = String.format("¥%.2f", uiState.balance),
                        color = if (uiState.balance >= 0) AccentLightGreen else AccentRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 饼图
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "支出分类",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PieChart(data = pieData)
                    }
                }
            }

            // 折线图
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "每日收支趋势",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LineChart(
                            transactions = uiState.transactions,
                            selectedYear = uiState.selectedYear,
                            selectedMonth = uiState.selectedMonth
                        )
                    }
                }
            }

            // 导出按钮
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box {
                        TextButton(onClick = { showExportMenu = true }) {
                            Text("导出数据", color = AccentGreen)
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("导出 CSV") },
                                onClick = {
                                    showExportMenu = false
                                    exportToCsv(context, viewModel)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("导出 Excel") },
                                onClick = {
                                    showExportMenu = false
                                    exportToExcel(context, viewModel)
                                }
                            )
                        }
                    }
                }
            }

            // 账单列表
            item {
                Text(
                    text = "账单列表",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
            }

            items(uiState.transactions) { transaction ->
                TransactionItem(
                    category = transaction.category,
                    date = viewModel.formatDate(transaction.date),
                    amount = transaction.amount,
                    isIncome = transaction.type == TransactionType.INCOME,
                    note = transaction.note,
                    onDelete = { viewModel.deleteTransaction(transaction) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
fun MonthSelector(
    year: Int,
    month: Int,
    onMonthChange: (Int, Int) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.clickable { showPicker = true },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = null,
                tint = AccentGreen
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$year 年 $month 月",
                style = MaterialTheme.typography.titleMedium,
                color = DarkText
            )
        }
    }

    if (showPicker) {
        MonthPickerDialog(
            initialYear = year,
            initialMonth = month,
            onConfirm = { y, m ->
                onMonthChange(y, m)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthPickerDialog(
    initialYear: Int,
    initialMonth: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedMonth by remember { mutableIntStateOf(initialMonth) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("选择月份", color = DarkText) },
        text = {
            Column {
                Text("年份: $selectedYear", color = DarkText)
                Slider(
                    value = selectedYear.toFloat(),
                    onValueChange = { selectedYear = it.toInt() },
                    valueRange = (initialYear - 2).toFloat()..initialYear.toFloat(),
                    steps = 2
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("月份: $selectedMonth", color = DarkText)
                Slider(
                    value = selectedMonth.toFloat(),
                    onValueChange = { selectedMonth = it.toInt() },
                    valueRange = 1f..12f,
                    steps = 10
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedYear, selectedMonth) }) {
                Text("确定", color = AccentGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = DarkTextSecondary)
            }
        }
    )
}

@Composable
fun AddTransactionDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var isIncome by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val categories = if (isIncome) viewModel.incomeCategories else viewModel.expenseCategories

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("添加账单", color = DarkText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 类型选择
                Row {
                    FilterChip(
                        selected = isIncome,
                        onClick = { isIncome = true; selectedCategory = "" },
                        label = { Text("收入") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGreen,
                            selectedLabelColor = DarkBackground
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = !isIncome,
                        onClick = { isIncome = false; selectedCategory = "" },
                        label = { Text("支出") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentRed,
                            selectedLabelColor = DarkBackground
                        )
                    )
                }

                // 分类
                Text("分类", color = DarkTextSecondary)
                var catIndex by remember { mutableIntStateOf(0) }
                categories.forEachIndexed { index, cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedCategory = cat
                                catIndex = index
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat; catIndex = index },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = if (isIncome) AccentGreen else AccentRed
                            )
                        )
                        Text(cat, color = DarkText)
                    }
                }

                // 金额
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("金额") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = DarkTextSecondary
                    )
                )

                // 日期
                OutlinedTextField(
                    value = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(Date(selectedDate)),
                    onValueChange = {},
                    label = { Text("日期") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "选择日期")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = DarkTextSecondary
                    )
                )

                // 备注
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = DarkTextSecondary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    if (selectedCategory.isNotEmpty() && amountValue > 0) {
                        viewModel.addTransaction(
                            isIncome = isIncome,
                            category = selectedCategory,
                            amount = amountValue,
                            date = selectedDate,
                            note = note.ifEmpty { null }
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = DarkTextSecondary)
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun exportToCsv(context: Context, viewModel: MainViewModel) {
    try {
        val transactions = viewModel.getExportData()
        val content = buildString {
            append("日期,类型,分类,金额,备注\n")
            transactions.forEach { t ->
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(t.date))
                val type = if (t.type == TransactionType.INCOME) "收入" else "支出"
                append("$date,$type,${t.category},${t.amount},${t.note ?: ""}\n")
            }
        }

        val fileName = "账单_${System.currentTimeMillis()}.csv"
        val file = File(context.getExternalFilesDir(null), fileName)
        FileOutputStream(file).use { it.write(content.toByteArray()) }

        shareFile(context, file)
        Toast.makeText(context, "CSV 导出成功", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun exportToExcel(context: Context, viewModel: MainViewModel) {
    try {
        // 简化的 Excel 导出 (CSV 格式被 Excel 兼容)
        val transactions = viewModel.getExportData()
        val content = buildString {
            append("日期\t类型\t分类\t金额\t备注\n")
            transactions.forEach { t ->
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(t.date))
                val type = if (t.type == TransactionType.INCOME) "收入" else "支出"
                append("$date\t$type\t${t.category}\t${t.amount}\t${t.note ?: ""}\n")
            }
        }

        val fileName = "账单_${System.currentTimeMillis()}.xls"
        val file = File(context.getExternalFilesDir(null), fileName)
        FileOutputStream(file).use { it.write(content.toByteArray()) }

        shareFile(context, file)
        Toast.makeText(context, "Excel 导出成功", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = if (file.name.endsWith(".xls")) "application/vnd.ms-excel" else "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "分享账单"))
}