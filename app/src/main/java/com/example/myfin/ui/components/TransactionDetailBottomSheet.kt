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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

private val MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailBottomSheet(
    transaction: TransactionEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
    onToggleSettled: ((txId: Long, isSettled: Boolean) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US) }

    val isCorporateInflow = remember(transaction.type, transaction.category) {
        (transaction.type == TransactionType.CORPORATE && transaction.category.trim().equals("Reimbursements & Claims", ignoreCase = true)) ||
                (transaction.type == TransactionType.INCOME && transaction.category.trim().equals("Reimbursements & Corporate Inflow", ignoreCase = true))
    }

    val isLegacyCorporateOutlay = remember(transaction.type, transaction.category) {
        transaction.type == TransactionType.EXPENSE && transaction.category.trim().equals("Work & Professional", ignoreCase = true)
    }

    val isCorporateOutlay = remember(transaction.type, transaction.category, isLegacyCorporateOutlay) {
        (transaction.type == TransactionType.CORPORATE && !transaction.category.trim().equals("Reimbursements & Claims", ignoreCase = true)) ||
                isLegacyCorporateOutlay
    }

    var isSettledState by remember(transaction.id, transaction.isSettled) {
        mutableStateOf(transaction.isSettled)
    }

    val (typeColor, typeBg) = when (transaction.type) {
        TransactionType.INCOME -> Success700 to Success100
        TransactionType.EXPENSE -> if (isLegacyCorporateOutlay) (Warning700 to Warning100) else (Error700 to Error100)
        TransactionType.ASSET -> SoftTeal to SoftTealLight
        TransactionType.CORPORATE -> if (isCorporateInflow) (Success700 to Success100) else (Warning700 to Warning100)
        TransactionType.TRANSFER -> Primary700 to Primary100
    }

    val amountColor = when (transaction.type) {
        TransactionType.INCOME -> Success500
        TransactionType.EXPENSE -> if (isLegacyCorporateOutlay) Warning700 else TextDark
        TransactionType.ASSET -> SoftTeal
        TransactionType.CORPORATE -> if (isCorporateInflow) Success500 else Warning700
        TransactionType.TRANSFER -> Primary600
    }

    val typeLabel = when (transaction.type) {
        TransactionType.INCOME -> if (isCorporateInflow) "Corporate Claim / Advance" else "Income Inflow"
        TransactionType.EXPENSE -> if (isLegacyCorporateOutlay) "Corporate Work Outlay" else "Discretionary Expense"
        TransactionType.ASSET -> "Wealth / SIP Investment"
        TransactionType.CORPORATE -> if (isCorporateInflow) "Corporate Claim / Advance" else "Corporate Work Outlay"
        TransactionType.TRANSFER -> "Internal Vault Sweep"
    }

    val amountPrefix = when (transaction.type) {
        TransactionType.EXPENSE -> "-"
        TransactionType.INCOME -> "+"
        TransactionType.ASSET -> "•"
        TransactionType.CORPORATE -> if (isCorporateInflow) "+" else "-"
        TransactionType.TRANSFER -> "⇄"
    }

    val friendlySubcategory = remember(transaction.subcategory, transaction.type) {
        if (transaction.type == TransactionType.TRANSFER) {
            when (transaction.subcategory.trim()) {
                "WEALTH_ALLOCATION" -> "Fortress Sweep"
                "BILL_FUNDING" -> "Bill Funding"
                "REBALANCE" -> "Vault Rebalance"
                "CASH_WITHDRAWAL" -> "Cash ATM Withdrawal"
                else -> transaction.subcategory.trim().ifBlank { "Vault Sweep" }
            }
        } else {
            transaction.subcategory.trim()
        }
    }

    val isAutoTransferTitle = remember(transaction.title, friendlySubcategory) {
        val cleanTitle = transaction.title.trim()
        cleanTitle.startsWith("Vault Transfer (", ignoreCase = true) ||
                cleanTitle.startsWith("Fortress Sweep (", ignoreCase = true) ||
                cleanTitle.startsWith("Bill Funding (", ignoreCase = true) ||
                cleanTitle.startsWith("Cash ATM Withdrawal (", ignoreCase = true) ||
                cleanTitle.startsWith("Vault Rebalance (", ignoreCase = true)
    }

    val displayHeroTitle = remember(transaction.title, friendlySubcategory, isAutoTransferTitle) {
        val cleanTitle = transaction.title.trim()
        val cleanSubcat = friendlySubcategory.trim()
        when {
            cleanTitle.isBlank() || cleanTitle.equals(cleanSubcat, ignoreCase = true) || isAutoTransferTitle -> {
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

    val distinctCustomNote = remember(transaction.title, friendlySubcategory, isAutoTransferTitle) {
        val cleanTitle = transaction.title.trim()
        val cleanSubcat = friendlySubcategory.trim()
        when {
            cleanTitle.isBlank() || cleanTitle.equals(cleanSubcat, ignoreCase = true) || isAutoTransferTitle -> null
            cleanTitle.startsWith(cleanSubcat, ignoreCase = true) -> {
                val unique = cleanTitle.removePrefix(cleanSubcat).trim(' ', '-', ':', '(', ')')
                unique.ifBlank { null }
            }
            else -> cleanTitle
        }
    }

    val relativeDateTag = remember(transaction.date) {
        val txCal = Calendar.getInstance().apply { timeInMillis = transaction.date }
        val nowCal = Calendar.getInstance()
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        val isToday = txCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                txCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        val isYesterday = txCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                txCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

        val isFuture = txCal.after(nowCal) && !isToday

        when {
            isToday -> "Today"
            isYesterday -> "Yesterday"
            isFuture -> "Scheduled"
            else -> "Past Entry"
        }
    }

    val resolvedTransferSubtype = remember(transaction) {
        if (transaction.type == TransactionType.TRANSFER) {
            if (transaction.transferSubtype != TransferSubtype.NONE) {
                transaction.transferSubtype
            } else {
                when (transaction.subcategory.trim()) {
                    "WEALTH_ALLOCATION" -> TransferSubtype.WEALTH_ALLOCATION
                    "BILL_FUNDING" -> TransferSubtype.BILL_FUNDING
                    "REBALANCE" -> TransferSubtype.REBALANCE
                    "CASH_WITHDRAWAL" -> TransferSubtype.CASH_WITHDRAWAL
                    else -> TransferSubtype.NONE
                }
            }
        } else TransferSubtype.NONE
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF2F5FF),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 4.dp)
                    .width(38.dp)
                    .height(4.dp),
                shape = CircleShape,
                color = Neutral300
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF2F5FF), Color(0xFFF8FAFC), CardWhite)
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 2.dp, bottom = 14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaction Details",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.5.sp,
                    color = TextDark
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(CardWhite.copy(alpha = 0.85f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextDark, modifier = Modifier.size(15.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compact Hero Block
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.85f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = typeBg
                        ) {
                            Text(
                                text = typeLabel.uppercase(),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = typeColor,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Primary50,
                            border = BorderStroke(0.5.dp, Primary200)
                        ) {
                            Text(
                                text = relativeDateTag.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary700,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                            )
                        }

                        if (isCorporateOutlay) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSettledState) Success100 else Warning100,
                                border = BorderStroke(0.5.dp, if (isSettledState) Success500.copy(alpha = 0.4f) else Warning500.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = if (isSettledState) "SETTLED" else "UNSETTLED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSettledState) Success700 else Warning700,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "$amountPrefix$currencySymbol${String.format(Locale.US, "%,.2f", transaction.amount)}",
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Black,
                        color = amountColor,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = displayHeroTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Corporate Claim Settlement Toggle Card
            if (isCorporateOutlay && onToggleSettled != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSettledState) Success100.copy(alpha = 0.55f) else Warning100.copy(alpha = 0.55f),
                    border = BorderStroke(
                        0.8.dp,
                        if (isSettledState) Success500.copy(alpha = 0.45f) else Warning500.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isSettledState) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (isSettledState) Success700 else Warning700,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isSettledState) "Claim Settled" else "Mark Claim as Settled",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = if (isSettledState) "Reimbursed by employer" else "Pending employer reimbursement",
                                    fontSize = 10.5.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Switch(
                            checked = isSettledState,
                            onCheckedChange = { checked ->
                                isSettledState = checked
                                onToggleSettled(transaction.id, checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Success500,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Neutral300
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compact Itemized Information Matrix
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.85f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)) {
                    DetailInfoRow(
                        icon = Icons.Default.Category,
                        label = "Category",
                        value = transaction.category
                    )
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.55f), thickness = 0.6.dp)

                    DetailInfoRow(
                        icon = Icons.Default.SubdirectoryArrowRight,
                        label = "Subcategory",
                        value = friendlySubcategory
                    )

                    if (distinctCustomNote != null) {
                        HorizontalDivider(color = BorderLight.copy(alpha = 0.55f), thickness = 0.6.dp)
                        DetailInfoRow(
                            icon = Icons.Default.EditNote,
                            label = "Custom Note",
                            value = distinctCustomNote
                        )
                    }

                    HorizontalDivider(color = BorderLight.copy(alpha = 0.55f), thickness = 0.6.dp)

                    DetailInfoRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        label = if (transaction.type == TransactionType.TRANSFER) "Vault Route" else "Account Vault",
                        value = if (transaction.type == TransactionType.TRANSFER && transaction.toAccountName != null) {
                            "${transaction.accountName} \u2794 ${transaction.toAccountName}"
                        } else {
                            transaction.accountName
                        }
                    )
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.55f), thickness = 0.6.dp)

                    if (transaction.type == TransactionType.TRANSFER && resolvedTransferSubtype != TransferSubtype.NONE) {
                        val subtypeLabel = when (resolvedTransferSubtype) {
                            TransferSubtype.BILL_FUNDING -> "Bill Funding"
                            TransferSubtype.WEALTH_ALLOCATION -> "Fortress Sweep"
                            TransferSubtype.REBALANCE -> "Vault Rebalance"
                            TransferSubtype.CASH_WITHDRAWAL -> "Cash ATM Withdrawal"
                            TransferSubtype.NONE -> "Unclassified"
                        }
                        DetailInfoRow(
                            icon = Icons.Default.SyncAlt,
                            label = "Classification",
                            value = subtypeLabel
                        )
                        HorizontalDivider(color = BorderLight.copy(alpha = 0.55f), thickness = 0.6.dp)
                    }

                    DetailInfoRow(
                        icon = Icons.Default.Schedule,
                        label = "Recorded",
                        value = dateFormatter.format(Date(transaction.date))
                    )
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.55f), thickness = 0.6.dp)

                    val monthName = MONTH_NAMES.getOrNull(transaction.month - 1) ?: "Month ${transaction.month}"
                    DetailInfoRow(
                        icon = Icons.Default.CalendarMonth,
                        label = "Cycle",
                        value = "$monthName ${transaction.year}"
                    )
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.55f), thickness = 0.6.dp)

                    DetailInfoRow(
                        icon = if (transaction.linkedFixedBillId != null) Icons.Default.EventRepeat else Icons.Default.EditNote,
                        label = "Commitment",
                        value = if (transaction.linkedFixedBillId != null) "Linked AutoPay" else "Standalone Entry"
                    )

                    if (transaction.type == TransactionType.CORPORATE || isCorporateInflow || isLegacyCorporateOutlay) {
                        HorizontalDivider(color = BorderLight.copy(alpha = 0.55f), thickness = 0.6.dp)
                        DetailInfoRow(
                            icon = Icons.Default.WorkOutline,
                            label = "Policy",
                            value = if (isCorporateInflow) {
                                "Excluded from personal income"
                            } else {
                                "Excluded from personal expenses"
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onDelete(transaction)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Error700),
                    border = BorderStroke(1.dp, Error500.copy(alpha = 0.5f))
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
                    modifier = Modifier
                        .weight(1.3f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary600)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Entry", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }
            }
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
            .padding(vertical = 8.dp),
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
                tint = Primary600.copy(alpha = 0.8f),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.3f)
        )
    }
}
