package com.example.myfin.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.FilterCriteria
import com.example.myfin.ui.theme.*

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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Filter Ledger Entries", fontWeight = FontWeight.Black, fontSize = 17.sp, color = TextDark)

            Spacer(modifier = Modifier.height(14.dp))

            Text("Flow Type", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedType == null,
                        onClick = { selectedType = null },
                        label = { Text("All", fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                items(TransactionType.entries) { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type.name, fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Vault Account", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedAccount == "ALL",
                        onClick = { selectedAccount = "ALL" },
                        label = { Text("All Accounts", fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                items(accountList) { acc ->
                    FilterChip(
                        selected = selectedAccount == acc,
                        onClick = { selectedAccount = acc },
                        label = { Text(acc, fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        onReset()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Reset")
                }
                Button(
                    onClick = {
                        onApply(selectedType, selectedAccount, currentFilter.startDate, currentFilter.endDate)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Text("Apply Filter", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
