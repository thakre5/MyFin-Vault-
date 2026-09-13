package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.myfin.data.FixedBillEntity
import com.example.myfin.data.TransactionEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.data.TransferSubtype
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.components.*
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar

internal val MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthlyScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToTaxonomy: () -> Unit = {},
    onNavigateToVaults: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.monthlyUiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val filterCriteria by viewModel.filterCriteria.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 3 })
    val (isDockVisible, scrollConnection) = rememberAutoScrollVisibilityConnection()
    val pageTitles = remember { listOf("Summary", "Ledger", "AutoPay") }

    var isDiscreetMode by remember { mutableStateOf(false) }
    var showStsInfoSheet by remember { mutableStateOf(false) }
    var dismissedWaterfallMonth by remember { mutableIntStateOf(0) }
    var dismissedSweepMonth by remember { mutableIntStateOf(0) }

    var showAddSheet by remember { mutableStateOf(false) }
    var showTransferSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }

    var hideSettledCommitments by remember { mutableStateOf(false) }
    var selectedCommitmentFilter by remember { mutableStateOf<TransactionType?>(null) }

    var viewingTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var showAddFixedBill by remember { mutableStateOf(false) }
    var editingFixedBill by remember { mutableStateOf<FixedBillEntity?>(null) }

    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var billToDelete by remember { mutableStateOf<FixedBillEntity?>(null) }
    var billToRevert by remember { mutableStateOf<FixedBillEntity?>(null) }
    var settlingFixedBill by remember { mutableStateOf<FixedBillEntity?>(null) }

    val activeAccounts = remember(uiState.activeAccounts, uiState.accounts) {
        uiState.activeAccounts.ifEmpty { uiState.accounts.filter { !it.isArchived } }
    }
    val accountsList = remember(activeAccounts) {
        activeAccounts.map { it.accountName }
    }

    val operatingAccountName = remember(activeAccounts) {
        activeAccounts.firstOrNull { it.accountType.equals("Operating", ignoreCase = true) }?.accountName
            ?: activeAccounts.firstOrNull()?.accountName ?: "PRIMARY BANK"
    }
    val commitmentsAccountName = remember(activeAccounts) {
        activeAccounts.firstOrNull { it.accountType.equals("Commitments", ignoreCase = true) }?.accountName
            ?: activeAccounts.getOrNull(1)?.accountName ?: "SECONDARY BANK"
    }
    val fortressAccountName = remember(activeAccounts) {
        activeAccounts.firstOrNull { it.accountType.equals("Fortress", ignoreCase = true) }?.accountName
            ?: activeAccounts.getOrNull(2)?.accountName ?: "TERTIARY BANK"
    }

    val todayCal = remember { Calendar.getInstance() }
    val isCurrentMonth = uiState.selectedYear == todayCal.get(Calendar.YEAR) &&
            uiState.selectedMonth == (todayCal.get(Calendar.MONTH) + 1)
    val isPastMonth = (uiState.selectedYear < todayCal.get(Calendar.YEAR)) ||
            (uiState.selectedYear == todayCal.get(Calendar.YEAR) && uiState.selectedMonth < (todayCal.get(Calendar.MONTH) + 1))

    val fabActions = remember {
        listOf(
            DockFabAction(
                icon = Icons.Default.Add,
                label = "Add Entry",
                onClick = {
                    editingTx = null
                    showAddSheet = true
                }
            ),
            DockFabAction(
                icon = Icons.Default.SyncAlt,
                label = "Transfer",
                onClick = { showTransferSheet = true }
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight)
            .nestedScroll(scrollConnection)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. PINNED TOP BAR
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(2f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CanvasLight)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(38.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Drawer / Navigation",
                            tint = TextDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showMonthPicker = true },
                        shape = RoundedCornerShape(20.dp),
                        color = CardWhite,
                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f)),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${MONTH_NAMES[uiState.selectedMonth - 1]} ${uiState.selectedYear}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pagerState.currentPage == 2) {
                            IconButton(
                                onClick = { hideSettledCommitments = !hideSettledCommitments },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (hideSettledCommitments) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                    contentDescription = "Toggle Settled Visibility",
                                    tint = if (hideSettledCommitments) AccentPurple else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { isDiscreetMode = !isDiscreetMode },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = if (isDiscreetMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Balance Privacy",
                                tint = if (isDiscreetMode) AccentPurple else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(CanvasLight, CanvasLight.copy(alpha = 0f))
                            )
                        )
                )
            }

            // 2. HORIZONTAL PAGER
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    when (page) {
                        0 -> MonthlySummaryTab(
                            viewModel = viewModel,
                            uiState = uiState,
                            userProfile = userProfile,
                            filterCriteria = filterCriteria,
                            isDiscreetMode = isDiscreetMode,
                            isCurrentMonth = isCurrentMonth,
                            isPastMonth = isPastMonth,
                            dismissedWaterfallMonth = dismissedWaterfallMonth,
                            dismissedSweepMonth = dismissedSweepMonth,
                            operatingAccountName = operatingAccountName,
                            commitmentsAccountName = commitmentsAccountName,
                            fortressAccountName = fortressAccountName,
                            onOpenStsInfo = { showStsInfoSheet = true },
                            onOpenTransferSheet = { showTransferSheet = true },
                            onDismissWaterfall = { dismissedWaterfallMonth = uiState.selectedMonth },
                            onDismissSweep = { dismissedSweepMonth = uiState.selectedMonth },
                            onNavigateToLedgerWithFilter = { type ->
                                coroutineScope.launch {
                                    viewModel.updateFilter(type, filterCriteria.account, filterCriteria.startDate, filterCriteria.endDate)
                                    pagerState.animateScrollToPage(1)
                                }
                            },
                            onNavigateToCommitments = {
                                coroutineScope.launch { pagerState.animateScrollToPage(2) }
                            }
                        )

                        1 -> MonthlyLedgerTab(
                            viewModel = viewModel,
                            uiState = uiState,
                            userProfile = userProfile,
                            filterCriteria = filterCriteria,
                            accountsList = accountsList,
                            isDiscreetMode = isDiscreetMode,
                            onOpenFilterSheet = { showFilterSheet = true },
                            onViewTx = { viewingTx = it },
                            onEditTx = { editingTx = it; showAddSheet = true },
                            onDeleteTx = { transactionToDelete = it }
                        )

                        2 -> MonthlyAutoPayTab(
                            uiState = uiState,
                            userProfile = userProfile,
                            isDiscreetMode = isDiscreetMode,
                            isCurrentMonth = isCurrentMonth,
                            isPastMonth = isPastMonth,
                            hideSettledCommitments = hideSettledCommitments,
                            selectedCommitmentFilter = selectedCommitmentFilter,
                            onSelectCommitmentFilter = { selectedCommitmentFilter = it },
                            onResetFilters = {
                                hideSettledCommitments = false
                                selectedCommitmentFilter = null
                            },
                            onAddAutoPay = { showAddFixedBill = true },
                            onTapBill = { bill ->
                                if (!bill.isPaid) settlingFixedBill = bill else billToRevert = bill
                            },
                            onEditBill = { bill ->
                                if (bill.isPaid) {
                                    Toast.makeText(context, "Cannot edit settled commitment. Tap card to revert to Unpaid first.", Toast.LENGTH_LONG).show()
                                } else {
                                    editingFixedBill = bill
                                }
                            },
                            onDeleteBill = { billToDelete = it },
                            onSettleBill = { bill, customAmt, dateMillis ->
                                viewModel.toggleFixedBillPaid(bill, customAmount = customAmt, customDateMillis = dateMillis)
                            }
                        )
                    }
                }
            }
        }

        // 3. FLOATING PAGER INDICATOR PILL
        FloatingPagerIndicator(
            pagerState = pagerState,
            pageTitles = pageTitles,
            isVisible = isDockVisible.value,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 22.dp, bottom = 78.dp)
                .zIndex(3.5f)
        )

        // 4. FLOATING BOTTOM NAVIGATION DOCK WITH FAB
        AppBottomDock(
            currentSelection = NavigationTarget.MONTHLY_VIEW,
            onSelectTarget = { target ->
                when (target) {
                    NavigationTarget.BUDGET_PLANNER -> onNavigateToPlanner()
                    NavigationTarget.VAULT_ACCOUNTS -> onNavigateToVaults()
                    NavigationTarget.REPORTS_ANALYTICS -> onNavigateToAnalytics()
                    NavigationTarget.DATA_SET -> onNavigateToTaxonomy()
                    NavigationTarget.MONTHLY_VIEW -> {}
                    else -> {}
                }
            },
            fabActions = fabActions,
            isVisible = isDockVisible.value,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(4f)
        )

        // 5. MODALS, SHEETS & CONFIRMATION DIALOGS
        viewingTx?.let { tx ->
            TransactionDetailBottomSheet(
                transaction = tx,
                currencySymbol = userProfile.currencySymbol,
                onDismiss = { viewingTx = null },
                onEdit = {
                    viewingTx = null
                    editingTx = it
                    showAddSheet = true
                },
                onDelete = {
                    viewingTx = null
                    transactionToDelete = it
                }
            )
        }

        transactionToDelete?.let { tx ->
            DeleteTransactionConfirmDialog(
                tx = tx,
                currencySymbol = userProfile.currencySymbol,
                onConfirm = {
                    viewModel.deleteTransaction(tx)
                    transactionToDelete = null
                },
                onDismiss = { transactionToDelete = null }
            )
        }

        billToDelete?.let { bill ->
            DeleteFixedBillConfirmDialog(
                bill = bill,
                onConfirm = {
                    viewModel.deleteFixedBill(bill)
                    billToDelete = null
                },
                onDismiss = { billToDelete = null }
            )
        }

        billToRevert?.let { bill ->
            RevertFixedBillConfirmDialog(
                bill = bill,
                onConfirm = {
                    viewModel.toggleFixedBillPaid(bill)
                    billToRevert = null
                },
                onDismiss = { billToRevert = null }
            )
        }

        settlingFixedBill?.let { bill ->
            SettleFixedBillDialog(
                bill = bill,
                activeAccounts = activeAccounts,
                currencySymbol = userProfile.currencySymbol,
                onDismiss = { settlingFixedBill = null },
                onConfirm = { amt, dateMillis ->
                    viewModel.toggleFixedBillPaid(bill, customAmount = amt, customDateMillis = dateMillis)
                    settlingFixedBill = null
                }
            )
        }

        if (showTransferSheet) {
            AccountTransferDialog(
                accounts = accountsList,
                currencySymbol = userProfile.currencySymbol,
                onDismiss = { showTransferSheet = false },
                onTransfer = { from, to, amount, note, subtype, date, isRecurring, dueDay ->
                    if (isRecurring) {
                        viewModel.addFixedBill(
                            title = note.ifBlank { "Vault Transfer ($from ➤ $to)" },
                            amount = amount,
                            category = "Transfer",
                            subcategory = subtype.name,
                            account = from,
                            toAccount = to,
                            type = TransactionType.TRANSFER,
                            dueDay = dueDay,
                            isPaid = true,
                            paidDateMillis = date
                        )
                        Toast.makeText(context, "Saved as recurring sweep & transferred ${userProfile.currencySymbol}${String.format(java.util.Locale.US, "%,.2f", amount)}", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.executeInstantTransfer(
                            fromAccount = from,
                            toAccount = to,
                            amount = amount,
                            note = note,
                            subtype = subtype,
                            date = date
                        )
                        Toast.makeText(context, "Transferred ${userProfile.currencySymbol}${String.format(java.util.Locale.US, "%,.2f", amount)}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        if (showMonthPicker) {
            MonthYearPickerDialog(
                selectedYear = uiState.selectedYear,
                selectedMonth = uiState.selectedMonth,
                onSelectYear = { viewModel.selectYear(it) },
                onSelectMonth = {
                    viewModel.selectMonth(it)
                    showMonthPicker = false
                },
                onDismiss = { showMonthPicker = false }
            )
        }

        if (showFilterSheet) {
            FilterBottomSheet(
                currentFilter = filterCriteria,
                accountList = accountsList,
                onDismiss = { showFilterSheet = false },
                onApply = { type, acc, start, end ->
                    viewModel.updateFilter(type, acc, start, end)
                },
                onReset = {
                    viewModel.resetFilters()
                }
            )
        }

        if (showAddSheet) {
            AddTransactionBottomSheet(
                editingTransaction = editingTx,
                currencySymbol = userProfile.currencySymbol,
                accountList = accountsList,
                masterCategories = uiState.masterCategories,
                masterSubcategories = uiState.masterSubcategories,
                onDismiss = { showAddSheet = false },
                onSave = { id, title, amount, category, subcat, acc, toAcc, type, date, isRecurring, dueDay ->
                    val resolvedSubtype = if (type == TransactionType.TRANSFER) {
                        try {
                            TransferSubtype.valueOf(subcat)
                        } catch (_: Exception) {
                            TransferSubtype.NONE
                        }
                    } else TransferSubtype.NONE

                    if (isRecurring && id == 0L) {
                        viewModel.addFixedBill(
                            title = title,
                            amount = amount,
                            category = category,
                            subcategory = subcat,
                            account = acc,
                            toAccount = toAcc,
                            type = type,
                            dueDay = dueDay,
                            isPaid = true,
                            paidDateMillis = date
                        )
                        Toast.makeText(context, "Saved as recurring AutoPay commitment & settled", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.saveTransaction(
                            id = id,
                            title = title,
                            amount = amount,
                            category = category,
                            subcategory = subcat,
                            accountName = acc,
                            type = type,
                            date = date,
                            toAccountName = if (type == TransactionType.TRANSFER) toAcc ?: editingTx?.toAccountName else null,
                            transferSubtype = resolvedSubtype
                        )
                    }

                    val cal = Calendar.getInstance().apply { timeInMillis = date }
                    val txMonth = cal.get(Calendar.MONTH) + 1
                    val txYear = cal.get(Calendar.YEAR)
                    if (txMonth != uiState.selectedMonth || txYear != uiState.selectedYear) {
                        Toast.makeText(context, "Logged to ${MONTH_NAMES[txMonth - 1]} $txYear ledger", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        if (showAddFixedBill) {
            AddEditFixedBillDialog(
                currencySymbol = userProfile.currencySymbol,
                accountList = accountsList,
                masterCategories = uiState.masterCategories,
                masterSubcategories = uiState.masterSubcategories,
                onAddNewCategory = { name, type -> viewModel.addCategory(name, type) },
                onAddNewSubcategory = { parent, name, type -> viewModel.addSubcategory(parent, name, type) },
                onDismiss = { showAddFixedBill = false },
                onSave = { title, amt, cat, subcat, acc, toAcc, type, dueDay, isPaid, paidDate ->
                    viewModel.addFixedBill(title, amt, cat, subcat, acc, toAcc, type, dueDay, isPaid, paidDate)
                    if (isPaid) {
                        val cal = Calendar.getInstance().apply { timeInMillis = paidDate }
                        val txMonth = cal.get(Calendar.MONTH) + 1
                        val txYear = cal.get(Calendar.YEAR)
                        if (txMonth != uiState.selectedMonth || txYear != uiState.selectedYear) {
                            Toast.makeText(context, "Settled in ${MONTH_NAMES[txMonth - 1]} $txYear ledger", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        editingFixedBill?.let { bill ->
            AddEditFixedBillDialog(
                initialBill = bill,
                currencySymbol = userProfile.currencySymbol,
                accountList = accountsList,
                masterCategories = uiState.masterCategories,
                masterSubcategories = uiState.masterSubcategories,
                onAddNewCategory = { name, type -> viewModel.addCategory(name, type) },
                onAddNewSubcategory = { parent, name, type -> viewModel.addSubcategory(parent, name, type) },
                onDismiss = { editingFixedBill = null },
                onSave = { title, amt, cat, subcat, acc, toAcc, type, dueDay, isPaid, paidDate ->
                    viewModel.updateFixedBill(bill.id, title, amt, cat, subcat, acc, toAcc, type, dueDay)
                    if (isPaid != bill.isPaid) {
                        viewModel.toggleFixedBillPaid(bill.copy(amount = amt), amt, paidDate)
                    }
                }
            )
        }

        if (showStsInfoSheet) {
            SafeToSpendInfoBottomSheet(
                uiState = uiState,
                userProfile = userProfile,
                onDismiss = { showStsInfoSheet = false }
            )
        }
    }
}
