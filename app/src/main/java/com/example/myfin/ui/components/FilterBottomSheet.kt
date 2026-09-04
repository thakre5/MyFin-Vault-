package com.example.myfin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.FilterCriteria
import com.example.myfin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private enum class DatePreset(val label: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    CUSTOM("Custom Range")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentFilter: FilterCriteria,
    accountList: List<String>,
    onDismiss: () -> Unit,
    onApply: (type: TransactionType?, account: String, startDate: Long?, endDate: Long?) -> Unit,
    onReset: () -> Unit
) {
    var selectedType by remember(currentFilter) { mutableStateOf(currentFilter.type) }
    var selectedAccount by remember(currentFilter) { mutableStateOf(currentFilter.account) }
    var filterStartDate by remember(currentFilter) { mutableStateOf(currentFilter.startDate) }
    var filterEndDate by remember(currentFilter) { mutableStateOf(currentFilter.endDate) }

    var showDateRangePicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Identify active date preset
    val activePreset = remember(filterStartDate, filterEndDate) {
        when {
            filterStartDate == null && filterEndDate == null -> DatePreset.ALL_TIME
            isPresetRange(filterStartDate, filterEndDate, DatePreset.TODAY) -> DatePreset.TODAY
            isPresetRange(filterStartDate, filterEndDate, DatePreset.YESTERDAY) -> DatePreset.YESTERDAY
            isPresetRange(filterStartDate, filterEndDate, DatePreset.THIS_MONTH) -> DatePreset.THIS_MONTH
            isPresetRange(filterStartDate, filterEndDate, DatePreset.LAST_MONTH) -> DatePreset.LAST_MONTH
            else -> DatePreset.CUSTOM
        }
    }

    val activeRangeText = remember(filterStartDate, filterEndDate) {
        if (filterStartDate != null && filterEndDate != null) {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
            "${sdf.format(Date(filterStartDate!!))} – ${sdf.format(Date(filterEndDate!!))}"
        } else null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp),
                shape = CircleShape,
                color = BorderLight
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter Ledger Entries",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = TextDark
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Flow Type Filter
            Text("Flow Type", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedType == null,
                        onClick = { selectedType = null },
                        label = { Text("All Flows", fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPurpleLight,
                            selectedLabelColor = AccentPurple
                        )
                    )
                }
                items(
                    listOf(
                        TransactionType.EXPENSE to "Expense",
                        TransactionType.INCOME to "Income",
                        TransactionType.ASSET to "Asset / SIP",
                        TransactionType.TRANSFER to "Transfer"
                    )
                ) { (type, label) ->
                    val isSelected = selectedType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedType = type },
                        label = { Text(label, fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPurpleLight,
                            selectedLabelColor = AccentPurple
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Vault Account Filter
            Text("Vault Account", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedAccount == "ALL",
                        onClick = { selectedAccount = "ALL" },
                        label = { Text("All Vaults", fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPurpleLight,
                            selectedLabelColor = AccentPurple
                        )
                    )
                }
                items(accountList) { acc ->
                    val isSelected = selectedAccount == acc
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedAccount = acc },
                        label = { Text(acc, fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPurpleLight,
                            selectedLabelColor = AccentPurple
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Date Range Presets
            Text("Date Window", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = activePreset == DatePreset.ALL_TIME,
                        onClick = {
                            filterStartDate = null
                            filterEndDate = null
                        },
                        label = { Text("All Time", fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPurpleLight,
                            selectedLabelColor = AccentPurple
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = activePreset == DatePreset.TODAY,
                        onClick = {
                            val range = calculatePresetRange(DatePreset.TODAY)
                            filterStartDate = range.first
                            filterEndDate = range.second
                        },
                        label = { Text("Today", fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPurpleLight,
                            selectedLabelColor = AccentPurple
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = activePreset == DatePreset.YESTERDAY,
                        onClick = {
                            val range = calculatePresetRange(DatePreset.YESTERDAY)
                            filterStartDate = range.first
                            filterEndDate = range.second
                        },
                        label = { Text("Yesterday", fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPurpleLight,
                            selectedLabelColor = AccentPurple
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = activePreset == DatePreset.THIS_MONTH,
                        onClick = {
                            val range = calculatePresetRange(DatePreset.THIS_MONTH)
                            filterStartDate = range.first
                            filterEndDate = range.second
                        },
                        label = { Text("This Month", fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPurpleLight,
                            selectedLabelColor = AccentPurple
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = activePreset == DatePreset.LAST_MONTH,
                        onClick = {
                            val range = calculatePresetRange(DatePreset.LAST_MONTH)
                            filterStartDate = range.first
                            filterEndDate = range.second
                        },
                        label = { Text("Last Month", fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPurpleLight,
                            selectedLabelColor = AccentPurple
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = activePreset == DatePreset.CUSTOM,
                        onClick = { showDateRangePicker = true },
                        label = { Text("Custom Range", fontSize = 11.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPurpleLight,
                            selectedLabelColor = AccentPurple
                        )
                    )
                }
            }

            // Active Date Filter Summary Bar
            if (activeRangeText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AccentPurpleLight,
                    border = BorderStroke(0.8.dp, AccentPurple.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = activeRangeText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentPurple
                            )
                        }
                        IconButton(
                            onClick = {
                                filterStartDate = null
                                filterEndDate = null
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Dates", tint = AccentPurple, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onReset()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark)
                ) {
                    Text("Reset All", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        onApply(selectedType, selectedAccount, filterStartDate, filterEndDate)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1.2f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Text("Apply Filter", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }

    // Material 3 Date Range Picker Dialog
    if (showDateRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = filterStartDate,
            initialSelectedEndDateMillis = filterEndDate
        )

        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startMillis = dateRangePickerState.selectedStartDateMillis
                        val endMillis = dateRangePickerState.selectedEndDateMillis ?: startMillis

                        if (startMillis != null) {
                            val utcCalStart = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = startMillis
                            }
                            val localStart = Calendar.getInstance().apply {
                                set(utcCalStart.get(Calendar.YEAR), utcCalStart.get(Calendar.MONTH), utcCalStart.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            filterStartDate = localStart.timeInMillis

                            val utcCalEnd = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = endMillis ?: startMillis
                            }
                            val localEnd = Calendar.getInstance().apply {
                                set(utcCalEnd.get(Calendar.YEAR), utcCalEnd.get(Calendar.MONTH), utcCalEnd.get(Calendar.DAY_OF_MONTH), 23, 59, 59)
                                set(Calendar.MILLISECOND, 999)
                            }
                            filterEndDate = localEnd.timeInMillis
                        }
                        showDateRangePicker = false
                    }
                ) {
                    Text("Apply Range", fontWeight = FontWeight.Bold, color = AccentPurple)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("Cancel", color = TextDark)
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        text = "Select Date Range",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            )
        }
    }
}

private fun calculatePresetRange(preset: DatePreset): Pair<Long, Long> {
    val cal = Calendar.getInstance()
    return when (preset) {
        DatePreset.TODAY -> {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            Pair(start, cal.timeInMillis)
        }
        DatePreset.YESTERDAY -> {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            Pair(start, cal.timeInMillis)
        }
        DatePreset.THIS_MONTH -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, maxDay)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            Pair(start, cal.timeInMillis)
        }
        DatePreset.LAST_MONTH -> {
            cal.add(Calendar.MONTH, -1)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, maxDay)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            Pair(start, cal.timeInMillis)
        }
        else -> Pair(0L, 0L)
    }
}

private fun isPresetRange(start: Long?, end: Long?, preset: DatePreset): Boolean {
    if (start == null || end == null) return false
    val (expectedStart, expectedEnd) = calculatePresetRange(preset)
    // Tolerate up to 1 second variance for clock shifts
    return abs(start - expectedStart) < 1000 && abs(end - expectedEnd) < 1000
}
