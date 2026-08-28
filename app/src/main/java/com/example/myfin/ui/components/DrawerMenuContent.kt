package com.example.myfin.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.theme.AccentPurple
import com.example.myfin.ui.theme.SoftRed
import java.io.File

@Composable
fun DrawerMenuContent(
    displayName: String,
    profileImageUri: String?,
    onUpdateProfileImageUri: (String) -> Unit,
    currentSelection: NavigationTarget,
    onSelectTarget: (NavigationTarget) -> Unit,
    onEditProfile: () -> Unit,
    onLockApp: () -> Unit
) {
    val context = LocalContext.current

    val profileBitmap = remember(profileImageUri) {
        if (!profileImageUri.isNullOrBlank()) {
            try {
                val file = File(profileImageUri)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } else {
                    val uri = Uri.parse(profileImageUri)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(245.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Profile Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onEditProfile() }
                    .padding(vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AccentPurple),
                    contentAlignment = Alignment.Center
                ) {
                    if (profileBitmap != null) {
                        Image(
                            bitmap = profileBitmap.asImageBitmap(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = displayName.take(1).uppercase().ifBlank { "M" },
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = displayName.ifBlank { "MyFin Vault" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Tap to manage profile",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Navigation Items (Bounded strictly inside the left pane)
            DrawerNavItem(
                icon = Icons.Default.Assessment,
                label = "Monthly Dashboard",
                isSelected = currentSelection == NavigationTarget.MONTHLY_VIEW,
                onClick = { onSelectTarget(NavigationTarget.MONTHLY_VIEW) }
            )
            DrawerNavItem(
                icon = Icons.Default.DateRange,
                label = "Annual Overview",
                isSelected = currentSelection == NavigationTarget.YEARLY_VIEW,
                onClick = { onSelectTarget(NavigationTarget.YEARLY_VIEW) }
            )
            DrawerNavItem(
                icon = Icons.Default.Tune,
                label = "Budget Planner",
                isSelected = currentSelection == NavigationTarget.BUDGET_PLANNER,
                onClick = { onSelectTarget(NavigationTarget.BUDGET_PLANNER) }
            )
            DrawerNavItem(
                icon = Icons.Default.Category,
                label = "Master Taxonomy",
                isSelected = currentSelection == NavigationTarget.DATA_SET,
                onClick = { onSelectTarget(NavigationTarget.DATA_SET) }
            )
            DrawerNavItem(
                icon = Icons.Default.AccountBalance,
                label = "Vault Accounts",
                isSelected = currentSelection == NavigationTarget.VAULT_ACCOUNTS,
                onClick = { onSelectTarget(NavigationTarget.VAULT_ACCOUNTS) }
            )
            DrawerNavItem(
                icon = Icons.Default.Analytics,
                label = "Reports & Analytics",
                isSelected = currentSelection == NavigationTarget.REPORTS_ANALYTICS,
                onClick = { onSelectTarget(NavigationTarget.REPORTS_ANALYTICS) }
            )
            DrawerNavItem(
                icon = Icons.Default.Settings,
                label = "Vault Settings",
                isSelected = currentSelection == NavigationTarget.SETTINGS,
                onClick = { onSelectTarget(NavigationTarget.SETTINGS) }
            )
            DrawerNavItem(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                label = "User Guide",
                isSelected = currentSelection == NavigationTarget.USER_GUIDE,
                onClick = { onSelectTarget(NavigationTarget.USER_GUIDE) }
            )
        }

        // Bottom Section: Centered Lock Action + Branding Footer
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SoftRed.copy(alpha = 0.15f),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onLockApp() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = SoftRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lock Vault",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = SoftRed
                    )
                }
            }

            AppBrandingFooter(
                modifier = Modifier.fillMaxWidth(),
                version = "v1.0.0",
                showIcon = true
            )
        }
    }
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) AccentPurple.copy(alpha = 0.28f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) AccentPurple else Color.White.copy(alpha = 0.82f),
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.82f)
            )
        }
    }
}
