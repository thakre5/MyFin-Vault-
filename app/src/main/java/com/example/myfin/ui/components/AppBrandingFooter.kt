package com.example.myfin.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.theme.TextMuted

@Composable
fun AppBrandingFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "MyFin Vault",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = TextMuted.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Offline • Encrypted • Multi-Vault Ledger",
            fontSize = 10.sp,
            color = TextMuted.copy(alpha = 0.6f)
        )
    }
}
