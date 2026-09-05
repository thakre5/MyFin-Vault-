package com.example.myfin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.TransactionEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.data.TransferSubtype
import com.example.myfin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailBottomSheet(
    transaction: TransactionEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.US) }
    val monthNameFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.US) }

    val typeColor = when (transaction.type) {
        TransactionType.INCOME -> SoftGreen
        TransactionType.EXPENSE -> SoftRed
        TransactionType.ASSET -> SoftTeal
        TransactionType.TRANSFER -> AccentPurple
    }

    val typeLabel = when (transaction.type) {
        TransactionType.INCOME -> "Income Inflow"
        TransactionType.EXPENSE -> "Discretionary Expense"
        TransactionType.ASSET -> "Wealth / SIP Investment"
        TransactionType.TRANSFER -> "Internal Vault Sweep"
    }

    val amountPrefix = when (transaction.type) {
        TransactionType.EXPENSE -> "-"
        TransactionType.INCOME -> "+"
        TransactionType.ASSET -> "•"
        TransactionType.TRANSFER -> "⇄"
    }

    // Map transfer enums to user-friendly titles
    val friendlySubcategory = remember(transaction.subcategory, transaction.type) {
        if (transaction.type == TransactionType.TRANSFER) {
            when (transaction.subcategory.trim()) {
                "WEALTH_ALLOCATION" -> "Fortress Sweep"
                "BILL_FUNDING" -> "Bill Funding"
                "REBALANCE" -> "Vault Rebalance"
                else -> transaction.subcategory.trim().ifBlank { "Vault Sweep" }
            }
        } else {
            transaction.subcategory.trim()
        }
    }

    // Standardized Primary Identification: Subcategory is primary; title only modifies if distinct
    val displayHeroTitle = remember(transaction.title, friendlySubcategory) {
        val cleanTitle = transaction.title.trim()
        val cleanSubcat = friendlySubcategory.trim()
        when {
            cleanTitle.isBlank() || cleanTitle.equals(cleanSubcat, ignoreCase = true) || cleanTitle.startsWith("Vault Transfer", ignoreCase = true) -> {
                cleanSubcat.ifBlank { cleanTitle.ifBlank { "Transaction" } }
            }
            cleanTitle.startsWith(cleanSubcat, ignoreCase = true) -> {
                val unique = cleanTitle.removePrefix(cleanSubcat).trim(' ', '-', ':', '(', ')')
                if (unique.isNotBlank()) "$cleanSubcat ($unique)" else cleanSubcat
            }
            cleanSubcat.isBlank() -> cleanTitle
            else -> "$cleanSubcat ($cleanTitle)"
        }
    }

    // Distinct custom note verification
    val distinctCustomNote = remember(transaction.title, friendlySubcategory) {
        val cleanTitle = transaction.title.trim()
        val cleanSubcat = friendlySubcategory.trim()
        when {
            cleanTitle.isBlank() || cleanTitle.equals(cleanSubcat, ignoreCase = true) || cleanTitle.startsWith("Vault Transfer", ignoreCase = true) -> null
            cleanTitle.startsWith(cleanSubcat, ignoreCase = true) -> {
                val unique = cleanTitle.removePrefix(cleanSubcat).trim(' ', '-', ':', '(', ')')
                unique.ifBlank { null }
            }
            else -> cleanTitle
        }
    }

    // Relative Day Label (Today, Yesterday, or Past Ledger)
    val relativeDateTag = remember(transaction.date) {
        val txCal = Calendar.getInstance().apply { timeInMillis = transaction.date }
        val nowCal = Calendar.getInstance()

        val isToday = txCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                txCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        val isYesterday = txCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                txCal.get(Calendar.DAY_OF_YEAR) == (nowCal.get(Calendar.DAY_OF_YEAR) - 1)

        when {
            isToday -> "Today"
            isYesterday -> "Yesterday"
            else -> "Logged Past Date"
        }
    }

    // Work / Corporate Reimbursement Pass-through Check
    val isReimbursableWorkOutlay = remember(transaction) {
        transaction.type == TransactionType.EXPENSE &&
        (transaction.category.equals("Work & Professional", ignoreCase = true) ||
         transaction.subcategory.contains("Work Travel", ignoreCase = true) ||
         transaction.subcategory.contains("Courier", ignoreCase = true) ||
         transaction.title.contains("Reimbursable", ignoreCase = true))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.5.dp),
                shape = CircleShape,
                color = BorderLight
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaction Details",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = TextDark
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CanvasLight)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextDark, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Hero Block (Amount & Title)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = CanvasLight,
                border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = typeColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = typeLabel.uppercase(),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Black,
                                color = typeColor,
                                letterSpacing = 0.6.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AccentPurple.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = relativeDateTag.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentPurple,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "$amountPrefix$currencySymbol${String.format(Locale.US, "%,.2f", transaction.amount)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = if (transaction.type == TransactionType.EXPENSE) TextDark else typeColor,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Primary Identification Headline
                    Text(
                        text = displayHeroTitle,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Itemized Information Matrix
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    DetailInfoRow(
                        icon = Icons.Default.Category,
                        label = "Category",
                        value = transaction.category
                    )
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.7.dp)

                    DetailInfoRow(
                        icon = Icons.Default.SubdirectoryArrowRight,
                        label = "Subcategory",
                        value = friendlySubcategory
                    )

                    // Show dedicated Custom Note row if entered
                    if (distinctCustomNote != null) {
                        HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.7.dp)
                        DetailInfoRow(
                            icon = Icons.Default.EditNote,
                            label = "Custom Note / Title",
                            value = distinctCustomNote
                        )
                    }

                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.7.dp)

                    DetailInfoRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        label = if (transaction.type == TransactionType.TRANSFER) "Vault Route" else "Account Vault",
                        value = if (transaction.type == TransactionType.TRANSFER && transaction.toAccountName != null) {
                            "${transaction.accountName} ➔ ${transaction.toAccountName}"
                        } else {
                            transaction.accountName
                        }
                    )
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.7.dp)

                    if (transaction.type == TransactionType.TRANSFER && transaction.transferSubtype != TransferSubtype.NONE) {
                        val subtypeLabel = when (transaction.transferSubtype) {
                            TransferSubtype.BILL_FUNDING -> "Bill Funding"
                            TransferSubtype.WEALTH_ALLOCATION -> "Fortress Sweep"
                            TransferSubtype.REBALANCE -> "Vault Rebalance"
                            TransferSubtype.CASH_WITHDRAWAL -> "Cash ATM Withdrawal"
                            TransferSubtype.NONE -> "Unclassified"
                        }
                        DetailInfoRow(
                            icon = Icons.Default.SyncAlt,
                            label = "Transfer Classification",
                            value = subtypeLabel
                        )
                        HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.7.dp)
                    }

                    DetailInfoRow(
                        icon = Icons.Default.Schedule,
                        label = "Recorded Date",
                        value = dateFormatter.format(Date(transaction.date))
                    )
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.7.dp)

                    // Target Accounting Ledger Cycle
                    val calTx = Calendar.getInstance().apply { timeInMillis = transaction.date }
                    DetailInfoRow(
                        icon = Icons.Default.CalendarMonth,
                        label = "Accounting Cycle",
                        value = monthNameFormatter.format(calTx.time)
                    )
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.7.dp)

                    DetailInfoRow(
                        icon = if (transaction.linkedFixedBillId != null) Icons.Default.EventRepeat else Icons.Default.EditNote,
                        label = "Commitment Link",
                        value = if (transaction.linkedFixedBillId != null) "Linked to Recurring AutoPay" else "Standalone Entry"
                    )

                    if (isReimbursableWorkOutlay) {
                        HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.7.dp)
                        DetailInfoRow(
                            icon = Icons.Default.WorkOutline,
                            label = "Reimbursement Policy",
                            value = "Excluded from personal lifestyle burn"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onDelete(transaction)
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftRed),
                    border = BorderStroke(1.dp, SoftRed.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        onDismiss()
                        onEdit(transaction)
                    },
                    modifier = Modifier.weight(1.3f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Entry", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun DetailInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.3f)
        )
    }
}
