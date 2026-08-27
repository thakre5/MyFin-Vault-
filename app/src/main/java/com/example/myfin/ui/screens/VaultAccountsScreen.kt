package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.AccountBalanceResult
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.components.AccountTransferDialog
import com.example.myfin.ui.theme.*
import java.util.Locale
import kotlin.math.abs

enum class VaultTier(val title: String, val description: String, val color: Color, val bgTint: Color) {
    OPERATING("Operating Vault", "Daily living & spending cashflow", Color(0xFFE57A28), Color(0xFFFFF0D4)),
    COMMITMENTS("Commitments Vault", "AutoPay, EMIs & fixed bills", AccentPurple, Color(0xFFF3E5F5)),
    FORTRESS("Emergency Fortress", "Untouchable safety net & liquid buffer", SoftTeal, Color(0xFFE0F7FA))
}

data class SuccessReceiptPayload(
    val subtitle: String,
    val headline: String,
    val description: String,
    val buttonText: String = "Done"
)

private fun getVaultTierForAccount(accountName: String): VaultTier {
    return when {
        accountName.contains("BOM", ignoreCase = true) ||
        accountName.contains("AXIS", ignoreCase = true) ||
        accountName.contains("SBI", ignoreCase = true) ||
        accountName.contains("BILL", ignoreCase = true) -> VaultTier.COMMITMENTS

        accountName.contains("INDUSIND", ignoreCase = true) ||
        accountName.contains("FORTRESS", ignoreCase = true) ||
        accountName.contains("FD", ignoreCase = true) ||
        accountName.contains("RESERVE", ignoreCase = true) -> VaultTier.FORTRESS

        else -> VaultTier.OPERATING
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultAccountsScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToTaxonomy: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.monthlyUiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var isThreeVaultStrategy by rememberSaveable { mutableStateOf(true) }
    var pendingModeTarget by remember { mutableStateOf<Boolean?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var receiptPayload by remember { mutableStateOf<SuccessReceiptPayload?>(null) }

    var showActionMenu by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showAddAccountSheet by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountBalanceResult?>(null) }

    val accountsList = remember(uiState.accounts) { uiState.accounts }
    val accountNames = remember(accountsList) {
        if (accountsList.isEmpty()) listOf("BOM", "CASH", "HDFC", "INDUSIND")
        else accountsList.map { it.accountName }
    }

    val totalLiquidBalance = remember(accountsList) { accountsList.sumOf { it.currentBalance } }
    val opTotal = remember(accountsList) {
        accountsList.filter { getVaultTierForAccount(it.accountName) == VaultTier.OPERATING }.sumOf { it.currentBalance }
    }
    val comTotal = remember(accountsList) {
        accountsList.filter { getVaultTierForAccount(it.accountName) == VaultTier.COMMITMENTS }.sumOf { it.currentBalance }
    }
    val fortTotal = remember(accountsList) {
        accountsList.filter { getVaultTierForAccount(it.accountName) == VaultTier.FORTRESS }.sumOf { it.currentBalance }
    }

    val totalAutoPayLiabilities = remember(uiState.fixedBills) {
        uiState.fixedBills.filter { !it.isPaid && it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }

    // Historical Trend Data Simulation
    val netWorthPoints = remember(uiState.metrics.dailyExpensePoints, totalLiquidBalance) {
        if (uiState.metrics.dailyExpensePoints.isNotEmpty() && uiState.metrics.dailyExpensePoints.size >= 5) {
            uiState.metrics.dailyExpensePoints
        } else {
            listOf(0.35f, 0.42f, 0.38f, 0.55f, 0.48f, 0.65f, 0.72f, 0.68f, 0.82f, 0.90f)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(CanvasLight)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Navigation Drawer Trigger
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(CardWhite)
                        .border(0.8.dp, BorderLight.copy(alpha = 0.7f), RoundedCornerShape(11.dp))
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Drawer",
                        tint = TextDark,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = if (isThreeVaultStrategy) "3-Vault Strategy" else "Vault Accounts",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = TextDark
                )

                // Top Right: Switch Mode Pill + Help Icon
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { pendingModeTarget = !isThreeVaultStrategy },
                        shape = RoundedCornerShape(10.dp),
                        color = CardWhite,
                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Switch",
                                tint = AccentPurple,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isThreeVaultStrategy) "3-Vault" else "Simple",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                    }

                    IconButton(
                        onClick = { showHelpDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CardWhite)
                            .border(0.8.dp, BorderLight.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.HelpOutline,
                            contentDescription = "Help Guide",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 105.dp)
            ) {
                // 1. Graphical Hero Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(22.dp)),
                        shape = RoundedCornerShape(22.dp),
                        color = CardWhite,
                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFFFFFFF),
                                            Color(0xFFFCFBFE),
                                            Color(0xFFF6F4FD)
                                        )
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TOTAL NET LIQUID WORTH",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextMuted,
                                    letterSpacing = 0.6.sp
                                )

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CanvasLight,
                                    border = BorderStroke(0.6.dp, BorderLight)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Month",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", totalLiquidBalance)}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextDark,
                                    letterSpacing = (-0.5).sp
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(7.dp),
                                    color = SoftGreen.copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = null,
                                            tint = SoftGreen,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "4.59%",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SoftGreen
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Interactive Area Chart
                            NetWorthAreaChart(
                                points = netWorthPoints,
                                lineColor = AccentPurple,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Timeline Markers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf("W1", "W2", "W3", "W4", "Current").forEach { dateTag ->
                                    Text(
                                        text = dateTag,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 2. Center Physical Bank Cards
                item {
                    Text(
                        text = if (isThreeVaultStrategy) "Strategic Vault Pillars" else "Connected Bank Accounts",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (isThreeVaultStrategy) {
                    // Horizontal Carousel for the 3 Strategic Tiers
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(VaultTier.values()) { tier ->
                                val tierAccs = accountsList.filter { getVaultTierForAccount(it.accountName) == tier }
                                val tierBalance = tierAccs.sumOf { it.currentBalance }

                                StrategicTierHeroCard(
                                    tier = tier,
                                    balance = tierBalance,
                                    currencySymbol = userProfile.currencySymbol,
                                    accountCount = tierAccs.size,
                                    modifier = Modifier.width(230.dp),
                                    onTap = {
                                        if (tierAccs.isNotEmpty()) {
                                            editingAccount = tierAccs.first()
                                        } else {
                                            showAddAccountSheet = true
                                        }
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Detailed Account Card List
                items(accountsList, key = { it.accountName }) { acc ->
                    val tier = getVaultTierForAccount(acc.accountName)

                    SwipeableAccountItem(
                        account = acc,
                        currencySymbol = userProfile.currencySymbol,
                        showRoleBadge = isThreeVaultStrategy,
                        roleTitle = tier.title.replace(" Vault", ""),
                        roleColor = tier.color,
                        onEdit = { editingAccount = acc },
                        onTransfer = { showTransferDialog = true }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 3. Vault Asset Allocation Analytics
                item {
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(22.dp)),
                        shape = RoundedCornerShape(22.dp),
                        color = CardWhite,
                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Vault Asset Allocation",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "Liquidity distribution across accounts",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }

                                IconButton(
                                    onClick = onNavigateToDashboard,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(CanvasLight)
                                ) {
                                    Icon(
                                        Icons.Default.NorthEast,
                                        contentDescription = "Analytics",
                                        tint = TextDark,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Donut Ring Chart with Legends
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val opFraction = if (totalLiquidBalance > 0) (opTotal / totalLiquidBalance).toFloat().coerceIn(0f, 1f) else 0.33f
                                val comFraction = if (totalLiquidBalance > 0) (comTotal / totalLiquidBalance).toFloat().coerceIn(0f, 1f) else 0.33f
                                val fortFraction = if (totalLiquidBalance > 0) (fortTotal / totalLiquidBalance).toFloat().coerceIn(0f, 1f) else 0.34f

                                Box(
                                    modifier = Modifier.size(110.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DonutAllocationChart(
                                        opFraction = opFraction,
                                        comFraction = comFraction,
                                        fortFraction = fortFraction,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Liquid", fontSize = 9.5.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", totalLiquidBalance)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = TextDark
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AllocationStatPill(
                                        title = "Operating",
                                        percentage = if (totalLiquidBalance > 0) ((opTotal / totalLiquidBalance) * 100).toInt() else 0,
                                        color = Color(0xFFE57A28),
                                        amount = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", opTotal)}"
                                    )
                                    AllocationStatPill(
                                        title = "Commitments",
                                        percentage = if (totalLiquidBalance > 0) ((comTotal / totalLiquidBalance) * 100).toInt() else 0,
                                        color = AccentPurple,
                                        amount = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", comTotal)}"
                                    )
                                    AllocationStatPill(
                                        title = "Fortress",
                                        percentage = if (totalLiquidBalance > 0) ((fortTotal / totalLiquidBalance) * 100).toInt() else 0,
                                        color = SoftTeal,
                                        amount = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", fortTotal)}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4 + 1 Floating Bottom Navigation Dock
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .shadow(16.dp, CircleShape),
                shape = CircleShape,
                color = CardWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DockPillTab(
                        title = "Taxonomy",
                        icon = Icons.Default.Category,
                        isSelected = false,
                        onClick = onNavigateToTaxonomy
                    )
                    DockPillTab(
                        title = "Planner",
                        icon = Icons.Default.PieChart,
                        isSelected = false,
                        onClick = onNavigateToPlanner
                    )
                    DockPillTab(
                        title = "Vaults",
                        icon = Icons.Default.AccountBalanceWallet,
                        isSelected = true,
                        onClick = { }
                    )
                    DockPillTab(
                        title = "Monthly",
                        icon = Icons.Default.Assessment,
                        isSelected = false,
                        onClick = onNavigateToDashboard
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            FloatingActionButton(
                onClick = { showActionMenu = !showActionMenu },
                containerColor = TextDark,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(60.dp).shadow(16.dp, CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Actions",
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(if (showActionMenu) 45f else 0f)
                )
            }
        }

        // Anchored Action Menu
        if (showActionMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showActionMenu = false }
                    )
            )

            AnimatedVisibility(
                visible = showActionMenu,
                enter = scaleIn(
                    transformOrigin = TransformOrigin(1f, 1f),
                    animationSpec = tween(180)
                ) + fadeIn(animationSpec = tween(180)),
                exit = scaleOut(
                    transformOrigin = TransformOrigin(1f, 1f),
                    animationSpec = tween(150)
                ) + fadeOut(animationSpec = tween(150)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 94.dp, end = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CardWhite,
                    shadowElevation = 10.dp,
                    border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f)),
                    modifier = Modifier.width(190.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showActionMenu = false
                                    showAddAccountSheet = true
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AddCard, contentDescription = null, tint = TextDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Add Account", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
                        }

                        HorizontalDivider(color = BorderLight.copy(alpha = 0.6f), thickness = 0.8.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showActionMenu = false
                                    showTransferDialog = true
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SyncAlt, contentDescription = null, tint = TextDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Transfer", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
                        }
                    }
                }
            }
        }

        // Help / Discipline Guide Dialog
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                title = { Text("3-Vault Strategy Guide", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("• Operating: For variable daily life and groceries. Never touch for bills.")
                        Text("• Commitments: Dedicated for AutoPay, loan EMIs, and monthly fixed bills.")
                        Text("• Fortress: Liquid emergency backup protecting you against unexpected surprises.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHelpDialog = false }) {
                        Text("Understood", fontWeight = FontWeight.Bold, color = AccentPurple)
                    }
                }
            )
        }

        // Mode Switch Confirmation Alert
        pendingModeTarget?.let { targetMode ->
            AlertDialog(
                onDismissRequest = { pendingModeTarget = null },
                title = {
                    Text(
                        text = if (targetMode) "Enable 3-Vault Strategy?" else "Switch to Simple Mode?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Text(
                        text = if (targetMode) {
                            "This organizes your accounts into Operating (daily spend), Commitments (AutoPay bills), and Emergency Fortress tiers. Your balances remain completely intact."
                        } else {
                            "This will display your accounts in a unified flat list without role compartmentalization."
                        },
                        fontSize = 13.sp,
                        color = TextDark
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isThreeVaultStrategy = targetMode
                            pendingModeTarget = null
                            receiptPayload = SuccessReceiptPayload(
                                subtitle = "Preference Updated",
                                headline = if (targetMode) "3-Vault Strategy Active" else "Simple Mode Active",
                                description = if (targetMode) {
                                    "Accounts structured into Operating, Commitments, and Fortress tiers."
                                } else {
                                    "Accounts structured into a flexible, flat liquidity list."
                                },
                                buttonText = "Done"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Confirm Switch", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingModeTarget = null }) {
                        Text("Cancel", color = TextDark)
                    }
                }
            )
        }

        // Instant Transfer Dialog
        if (showTransferDialog) {
            AccountTransferDialog(
                accounts = accountNames,
                onDismiss = { showTransferDialog = false },
                onTransfer = { from, to, amount, note ->
                    viewModel.executeInstantTransfer(from, to, amount, note)
                    receiptPayload = SuccessReceiptPayload(
                        subtitle = "Transfer Successful",
                        headline = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", amount)}",
                        description = "$from ➔ $to ${if (note.isNotBlank()) "($note)" else ""}",
                        buttonText = "View Vaults"
                    )
                }
            )
        }

        // Success Receipt Bottom Sheet (Matching Attached Reference)
        receiptPayload?.let { payload ->
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { receiptPayload = null },
                sheetState = sheetState,
                containerColor = CardWhite,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                dragHandle = {
                    Surface(
                        modifier = Modifier.padding(vertical = 10.dp).width(40.dp).height(4.dp),
                        shape = CircleShape,
                        color = BorderLight
                    ) {}
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AccentPurple,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = payload.subtitle,
                        fontSize = 12.5.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = payload.headline,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = payload.description,
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { receiptPayload = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextDark)
                    ) {
                        Text(
                            text = payload.buttonText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Add Account Bottom Sheet with Richer Account Types
        if (showAddAccountSheet) {
            var name by remember { mutableStateOf("") }
            var balanceText by remember { mutableStateOf("") }
            var selectedType by remember { mutableStateOf("Savings") }
            var selectedTier by remember { mutableStateOf(VaultTier.OPERATING) }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { showAddAccountSheet = false },
                sheetState = sheetState,
                containerColor = CardWhite,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Surface(modifier = Modifier.padding(vertical = 10.dp).width(40.dp).height(4.dp), shape = CircleShape, color = BorderLight) {}
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 22.dp, vertical = 6.dp)
                ) {
                    Text("Add Vault Account", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Configure account details and strategic tier assignment", fontSize = 11.5.sp, color = TextMuted)

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Account Name (e.g., HDFC Salary, ICICI)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Account Type Chips
                    Text("Account Type", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("Savings", "Salary", "Current", "Credit Card", "Cash Wallet", "Fixed Deposit")) { type ->
                            val isSel = selectedType == type
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedType = type },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) AccentPurple.copy(alpha = 0.12f) else CanvasLight,
                                border = BorderStroke(0.6.dp, if (isSel) AccentPurple else BorderLight)
                            ) {
                                Text(
                                    text = type,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) AccentPurple else TextDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3-Vault Strategic Role
                    Text("Strategic Vault Role", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VaultTier.values().forEach { tier ->
                            val isSel = selectedTier == tier
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { selectedTier = tier },
                                shape = RoundedCornerShape(9.dp),
                                color = if (isSel) tier.color.copy(alpha = 0.14f) else CanvasLight,
                                border = BorderStroke(0.7.dp, if (isSel) tier.color else BorderLight)
                            ) {
                                Text(
                                    text = tier.title.replace(" Vault", "").replace("Emergency ", ""),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) tier.color else TextDark,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = { Text("Initial Balance (${userProfile.currencySymbol})", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val bal = balanceText.toDoubleOrNull() ?: 0.0
                                val initialAmount = if (bal > 0.0) bal else 0.0
                                viewModel.saveTransaction(
                                    id = 0L,
                                    title = if (bal > 0.0) "Opening Balance" else "Account Initialized",
                                    amount = initialAmount,
                                    category = "General",
                                    subcategory = "Opening Balance",
                                    accountName = name.trim().uppercase(),
                                    type = TransactionType.INCOME,
                                    date = System.currentTimeMillis()
                                )
                                showAddAccountSheet = false
                                Toast.makeText(context, "Account '${name.trim().uppercase()}' initialized", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Adjust Account Balance Sheet
        editingAccount?.let { acc ->
            var newBalanceText by remember(acc) {
                mutableStateOf(String.format(Locale.US, "%.0f", acc.currentBalance))
            }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { editingAccount = null },
                sheetState = sheetState,
                containerColor = CardWhite,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Surface(modifier = Modifier.padding(vertical = 10.dp).width(40.dp).height(4.dp), shape = CircleShape, color = BorderLight) {}
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 22.dp, vertical = 6.dp)
                ) {
                    Text("Adjust: ${acc.accountName}", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Set updated balance. A ledger adjustment will be recorded.", fontSize = 11.5.sp, color = TextMuted)

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newBalanceText,
                        onValueChange = { newBalanceText = it },
                        label = { Text("Current Balance (${userProfile.currencySymbol})", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val targetBal = newBalanceText.toDoubleOrNull() ?: acc.currentBalance
                            val diff = targetBal - acc.currentBalance
                            if (diff != 0.0) {
                                val txType = if (diff > 0.0) TransactionType.INCOME else TransactionType.EXPENSE
                                val amountVal = if (diff < 0.0) -diff else diff
                                viewModel.saveTransaction(
                                    id = 0L,
                                    title = "Balance Adjustment",
                                    amount = amountVal,
                                    category = "General",
                                    subcategory = "Adjustment",
                                    accountName = acc.accountName,
                                    type = txType,
                                    date = System.currentTimeMillis()
                                )
                            }
                            editingAccount = null
                            Toast.makeText(context, "Balance adjusted for ${acc.accountName}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Save Balance", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun StrategicTierHeroCard(
    tier: VaultTier,
    balance: Double,
    currencySymbol: String,
    accountCount: Int,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onTap)
            .shadow(2.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(tier.bgTint.copy(alpha = 0.35f), CardWhite)
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(tier.bgTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (tier) {
                            VaultTier.OPERATING -> Icons.Default.ShoppingCart
                            VaultTier.COMMITMENTS -> Icons.Default.CreditCard
                            VaultTier.FORTRESS -> Icons.Default.Security
                        },
                        contentDescription = null,
                        tint = tier.color,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CanvasLight,
                    border = BorderStroke(0.5.dp, BorderLight)
                ) {
                    Text(
                        text = "$accountCount Acc",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = tier.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "$currencySymbol${String.format(Locale.US, "%,.2f", balance)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = tier.color
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = tier.description,
                fontSize = 9.5.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NetWorthAreaChart(
    points: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas

        val maxVal = points.maxOrNull() ?: 1f
        val minVal = points.minOrNull() ?: 0f
        val range = (maxVal - minVal).coerceAtLeast(0.001f)

        val width = size.width
        val height = size.height
        val stepX = width / (points.size - 1).coerceAtLeast(1)

        val path = Path()
        val fillPath = Path()

        points.forEachIndexed { i, p ->
            val norm = (p - minVal) / range
            val x = i * stepX
            val y = height - (norm * (height - 20.dp.toPx())) - 10.dp.toPx()

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (i - 1) * stepX
                val prevNorm = (points[i - 1] - minVal) / range
                val prevY = height - (prevNorm * (height - 20.dp.toPx())) - 10.dp.toPx()

                val cx = (prevX + x) / 2
                path.cubicTo(cx, prevY, cx, y, x, y)
                fillPath.cubicTo(cx, prevY, cx, y, x, y)
            }

            if (i == points.lastIndex) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        // Draw Gradient Fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw Continuous Line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // End Milestone Dot
        val lastNorm = (points.last() - minVal) / range
        val lastX = (points.size - 1) * stepX
        val lastY = height - (lastNorm * (height - 20.dp.toPx())) - 10.dp.toPx()

        drawCircle(color = CardWhite, radius = 5.dp.toPx(), center = Offset(lastX, lastY))
        drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = Offset(lastX, lastY))
    }
}

@Composable
private fun DonutAllocationChart(
    opFraction: Float,
    comFraction: Float,
    fortFraction: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 11.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val arcSize = Size(diameter, diameter)
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)

        val total = (opFraction + comFraction + fortFraction).coerceAtLeast(0.001f)
        val opAngle = (opFraction / total) * 360f
        val comAngle = (comFraction / total) * 360f
        val fortAngle = (fortFraction / total) * 360f

        var startAngle = -90f

        // Operating Arc
        drawArc(
            color = Color(0xFFE57A28),
            startAngle = startAngle,
            sweepAngle = (opAngle - 4f).coerceAtLeast(2f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        startAngle += opAngle

        // Commitments Arc
        drawArc(
            color = AccentPurple,
            startAngle = startAngle,
            sweepAngle = (comAngle - 4f).coerceAtLeast(2f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        startAngle += comAngle

        // Fortress Arc
        drawArc(
            color = SoftTeal,
            startAngle = startAngle,
            sweepAngle = (fortAngle - 4f).coerceAtLeast(2f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun AllocationStatPill(
    title: String,
    percentage: Int,
    color: Color,
    amount: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = CanvasLight,
        border = BorderStroke(0.6.dp, BorderLight.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.width(4.dp))
                Text("$percentage%", fontSize = 10.sp, color = TextMuted)
            }
            Text(amount, fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextDark)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableAccountItem(
    account: AccountBalanceResult,
    currencySymbol: String,
    showRoleBadge: Boolean = false,
    roleTitle: String = "Operating",
    roleColor: Color = AccentPurple,
    onEdit: () -> Unit,
    onTransfer: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val currentOnEdit by rememberUpdatedState(onEdit)
    val currentOnTransfer by rememberUpdatedState(onTransfer)

    var lastTargetValue by remember { mutableStateOf(SwipeToDismissBoxValue.Settled) }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.35f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnEdit()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnTransfer()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue != lastTargetValue && dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        lastTargetValue = dismissState.targetValue
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val backgroundColor by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> AccentPurple
                    SwipeToDismissBoxValue.EndToStart -> SoftTeal
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                },
                animationSpec = tween(200),
                label = "accountSwipeBg"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(backgroundColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = "Adjust", tint = Color.White, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Adjust", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Transfer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.SyncAlt, contentDescription = "Transfer", tint = Color.White, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(16.dp))
                .clickable(onClick = onEdit),
            shape = RoundedCornerShape(16.dp),
            color = CardWhite,
            border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    account.accountName.contains("HDFC", true) -> Color(0xFFE8F0FE)
                                    account.accountName.contains("BOM", true) -> Color(0xFFFFF3E0)
                                    account.accountName.contains("INDUSIND", true) -> Color(0xFFF3E5F5)
                                    account.accountName.contains("CASH", true) -> Color(0xFFE8F5E9)
                                    else -> AccentPurple.copy(alpha = 0.12f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                account.accountName.contains("CASH", true) -> Icons.Default.Payments
                                else -> Icons.Default.AccountBalance
                            },
                            contentDescription = null,
                            tint = when {
                                account.accountName.contains("HDFC", true) -> Color(0xFF1A73E8)
                                account.accountName.contains("BOM", true) -> Color(0xFFE65100)
                                account.accountName.contains("INDUSIND", true) -> Color(0xFF7B1FA2)
                                account.accountName.contains("CASH", true) -> Color(0xFF2E7D32)
                                else -> AccentPurple
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = account.accountName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (showRoleBadge) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = roleColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = roleTitle,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = roleColor,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Swipe to adjust or transfer",
                            fontSize = 10.5.sp,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "$currencySymbol${String.format(Locale.US, "%,.2f", account.currentBalance)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.5.sp,
                    color = if (account.currentBalance >= 0) TextDark else SoftRed
                )
            }
        }
    }
}

@Composable
private fun DockPillTab(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) CanvasLight else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isSelected) AccentPurple else TextMuted,
                modifier = Modifier.size(17.dp)
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(5.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = AccentPurple)
            }
        }
    }
}
