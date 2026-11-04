package com.example.myfin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.BudgetDao
import com.example.myfin.data.NoteEntity
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NoteChecklistItem(
    val text: String,
    val amount: Double = 0.0,
    val isChecked: Boolean = false
)

private val NOTE_TAGS = listOf("All", "General", "To-Buy", "IOU / Split", "Tax & SIP", "Goals")

private fun tagAccentColor(tag: String): Color = when (tag) {
    "To-Buy" -> SoftAmber
    "IOU / Split" -> SoftRed
    "Tax & SIP" -> SoftTeal
    "Goals" -> Color(0xFF4F46E5)
    else -> AccentPurple
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    budgetDao: BudgetDao,
    currencySymbol: String = "₹",
    onOpenDrawer: () -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val allNotes by budgetDao.getAllNotes().collectAsState(initial = emptyList())

    var selectedTag by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var defaultNewChecklistMode by remember { mutableStateOf(false) }
    var showEditorSheet by remember { mutableStateOf(false) }

    val filteredNotes = remember(allNotes, selectedTag, searchQuery) {
        allNotes.filter { note ->
            val matchesTag = selectedTag == "All" || note.tag.equals(selectedTag, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    note.title.contains(searchQuery, ignoreCase = true) ||
                    note.content.contains(searchQuery, ignoreCase = true)
            matchesTag && matchesQuery
        }
    }

    // Calculate total pending checklist value across all notes
    val totalPendingChecklistValue = remember(allNotes) {
        allNotes.filter { it.isChecklist }.sumOf { note ->
            parseChecklistJson(note.checklistJson).filter { !it.isChecked }.sumOf { it.amount }
        }
    }
    val pinnedCount = remember(allNotes) { allNotes.count { it.isPinned } }

    Scaffold(
        containerColor = CanvasLight,
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary Quick-Checklist FAB
                SmallFloatingActionButton(
                    onClick = {
                        editingNote = null
                        defaultNewChecklistMode = true
                        showEditorSheet = true
                    },
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Checklist, contentDescription = "New Checklist", modifier = Modifier.size(20.dp))
                }

                // Primary Note FAB
                ExtendedFloatingActionButton(
                    onClick = {
                        editingNote = null
                        defaultNewChecklistMode = false
                        showEditorSheet = true
                    },
                    containerColor = AccentPurple,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = "New Note")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Note", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            // 1. COMPACT TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Drawer",
                            tint = TextDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Financial Workspace",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = TextDark,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = "Memos, Checklists & IOU Ledgers",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(CardWhite)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Back",
                        tint = TextDark,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // 2. MIDNIGHT WORKSPACE OVERVIEW DECK
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PENDING CHECKLIST ESTIMATES",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFA5B4FC),
                            letterSpacing = 0.7.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.0f", totalPendingChecklistValue)}",
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WorkspaceStatChip(label = "Notes", count = allNotes.size.toString(), tint = Color(0xFF38BDF8))
                        WorkspaceStatChip(label = "Pinned", count = pinnedCount.toString(), tint = Color(0xFFFBBF24))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. SLEEK SEARCH BAR + TAG PILLS
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.85f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text("Search memos, tags, or checklists...", fontSize = 12.5.sp, color = TextMuted)
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 13.sp, color = TextDark, fontWeight = FontWeight.Medium),
                            cursorBrush = SolidColor(AccentPurple),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                NOTE_TAGS.forEach { tag ->
                    val isSelected = selectedTag == tag
                    val chipColor = if (tag == "All") AccentPurple else tagAccentColor(tag)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) chipColor else CardWhite,
                        border = BorderStroke(0.8.dp, if (isSelected) chipColor else BorderLight),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedTag = tag }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (tag != "All") {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else chipColor)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                            Text(
                                text = tag,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. NOTES LIST OR STARTER TEMPLATES
            if (filteredNotes.isEmpty()) {
                StarterTemplatesEmptyState(
                    onCreateFromTemplate = { templateTitle, templateTag, isChecklist, items ->
                        coroutineScope.launch {
                            budgetDao.insertNote(
                                NoteEntity(
                                    title = templateTitle,
                                    content = "",
                                    tag = templateTag,
                                    isPinned = false,
                                    isChecklist = isChecklist,
                                    checklistJson = encodeChecklistJson(items),
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        NoteCardItem(
                            note = note,
                            currencySymbol = currencySymbol,
                            onClick = {
                                editingNote = note
                                defaultNewChecklistMode = note.isChecklist
                                showEditorSheet = true
                            },
                            onTogglePin = {
                                coroutineScope.launch {
                                    budgetDao.updateNotePin(note.id, !note.isPinned)
                                }
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    budgetDao.deleteNote(note)
                                }
                            },
                            onToggleChecklistItem = { itemIndex ->
                                val list = parseChecklistJson(note.checklistJson).toMutableList()
                                if (itemIndex in list.indices) {
                                    val target = list[itemIndex]
                                    list[itemIndex] = target.copy(isChecked = !target.isChecked)
                                    coroutineScope.launch {
                                        budgetDao.insertNote(
                                            note.copy(
                                                checklistJson = encodeChecklistJson(list),
                                                updatedAt = System.currentTimeMillis()
                                            )
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showEditorSheet) {
        NoteEditorModalSheet(
            initialNote = editingNote,
            defaultChecklistMode = defaultNewChecklistMode,
            currencySymbol = currencySymbol,
            onDismiss = {
                showEditorSheet = false
                editingNote = null
            },
            onSave = { updatedNote ->
                coroutineScope.launch {
                    budgetDao.insertNote(updatedNote)
                }
                showEditorSheet = false
                editingNote = null
            }
        )
    }
}

@Composable
private fun WorkspaceStatChip(label: String, count: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count, fontSize = 14.sp, fontWeight = FontWeight.Black, color = tint)
            Text(text = label, fontSize = 9.5.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StarterTemplatesEmptyState(
    onCreateFromTemplate: (String, String, Boolean, List<NoteChecklistItem>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = CardWhite,
            border = BorderStroke(0.8.dp, BorderLight)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AccentPurple.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Start with a 1-Tap Template",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Or tap '+ New Note' below to create a blank memo or auto-summing checklist.",
                    fontSize = 11.5.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(14.dp))

                TemplateQuickRow(
                    icon = Icons.Default.ShoppingCart,
                    title = "Planned Purchases & Upgrades",
                    subtitle = "Auto-summing shopping & wishlist tracker",
                    tint = SoftAmber,
                    onClick = {
                        onCreateFromTemplate(
                            "Planned Purchases",
                            "To-Buy",
                            true,
                            listOf(
                                NoteChecklistItem("Home Router / Accessories", 2500.0, false),
                                NoteChecklistItem("Monthly Bulk Groceries", 4000.0, false)
                            )
                        )
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TemplateQuickRow(
                    icon = Icons.Default.SwapHoriz,
                    title = "IOU & Split Settlements",
                    subtitle = "Track money lent to friends or pending splits",
                    tint = SoftRed,
                    onClick = {
                        onCreateFromTemplate(
                            "Pending IOUs & Splits",
                            "IOU / Split",
                            true,
                            listOf(
                                NoteChecklistItem("Dinner Split Receivable", 850.0, false),
                                NoteChecklistItem("Shared Cab / Travel", 400.0, false)
                            )
                        )
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TemplateQuickRow(
                    icon = Icons.Default.VerifiedUser,
                    title = "Tax & Investment Checklist",
                    subtitle = "Track annual SIP step-ups, ELSS & insurance renewals",
                    tint = SoftTeal,
                    onClick = {
                        onCreateFromTemplate(
                            "Tax & SIP Checklist",
                            "Tax & SIP",
                            true,
                            listOf(
                                NoteChecklistItem("Index Fund SIP Step-Up", 5000.0, false),
                                NoteChecklistItem("Health Insurance Renewal", 15000.0, false)
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun TemplateQuickRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CanvasLight,
        border = BorderStroke(0.7.dp, BorderLight.copy(alpha = 0.8f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(text = subtitle, fontSize = 10.5.sp, color = TextMuted)
            }
            Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun NoteCardItem(
    note: NoteEntity,
    currencySymbol: String,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onToggleChecklistItem: (Int) -> Unit
) {
    val checklistItems = remember(note.checklistJson) {
        if (note.isChecklist) parseChecklistJson(note.checklistJson) else emptyList()
    }

    val totalAmount = remember(checklistItems) { checklistItems.sumOf { it.amount } }
    val pendingAmount = remember(checklistItems) { checklistItems.filter { !it.isChecked }.sumOf { it.amount } }
    val checkedCount = remember(checklistItems) { checklistItems.count { it.isChecked } }
    val accent = tagAccentColor(note.tag)

    val dateFormatted = remember(note.updatedAt) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(note.updatedAt))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.5.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = CardWhite,
        border = BorderStroke(
            width = if (note.isPinned) 1.2.dp else 0.8.dp,
            color = if (note.isPinned) accent.copy(alpha = 0.6f) else BorderLight.copy(alpha = 0.7f)
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Left Category Accent Bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accent)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (note.isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = accent,
                                modifier = Modifier
                                    .size(15.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = note.title.ifBlank { "Untitled Note" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accent.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = note.tag,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pin",
                                tint = if (note.isPinned) accent else TextMuted.copy(alpha = 0.45f),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = SoftRed.copy(alpha = 0.75f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (note.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.content,
                        fontSize = 12.5.sp,
                        color = TextMuted,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (note.isChecklist && checklistItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress Bar
                    val progress = (checkedCount.toFloat() / checklistItems.size.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = SoftGreen,
                        trackColor = BorderLight.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CanvasLight)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        checklistItems.take(5).forEachIndexed { idx, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleChecklistItem(idx) },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (item.isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (item.isChecked) SoftGreen else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.text,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (item.isChecked) TextMuted else TextDark,
                                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (item.amount > 0) {
                                    Text(
                                        text = "$currencySymbol${String.format(Locale.US, "%,.0f", item.amount)}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isChecked) TextMuted else TextDark
                                    )
                                }
                            }
                        }

                        if (checklistItems.size > 5) {
                            Text(
                                text = "+${checklistItems.size - 5} more items...",
                                fontSize = 10.5.sp,
                                color = accent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (note.isChecklist && checklistItems.isNotEmpty()) {
                            "$checkedCount/${checklistItems.size} done • $dateFormatted"
                        } else dateFormatted,
                        fontSize = 10.sp,
                        color = TextMuted
                    )

                    if (note.isChecklist && totalAmount > 0.0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (pendingAmount > 0) SoftAmber.copy(alpha = 0.14f) else SoftGreen.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = if (pendingAmount > 0) {
                                    "$currencySymbol${String.format(Locale.US, "%,.0f", pendingAmount)} Pending"
                                } else {
                                    "All Settled ($currencySymbol${String.format(Locale.US, "%,.0f", totalAmount)})"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (pendingAmount > 0) SoftAmber else SoftGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorModalSheet(
    initialNote: NoteEntity?,
    defaultChecklistMode: Boolean = false,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (NoteEntity) -> Unit
) {
    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var content by remember { mutableStateOf(initialNote?.content ?: "") }
    var tag by remember { mutableStateOf(initialNote?.tag ?: "General") }
    var isPinned by remember { mutableStateOf(initialNote?.isPinned ?: false) }
    var isChecklist by remember { mutableStateOf(initialNote?.isChecklist ?: defaultChecklistMode) }

    val checklistItems = remember {
        mutableStateListOf<NoteChecklistItem>().apply {
            if (initialNote != null && initialNote.isChecklist) {
                addAll(parseChecklistJson(initialNote.checklistJson))
            }
        }
    }

    var newItemText by remember { mutableStateOf("") }
    var newItemAmount by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialNote == null) "New Workspace Entry" else "Edit Entry",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = TextDark
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = isChecklist,
                        onClick = { isChecklist = !isChecklist },
                        label = { Text("Checklist + Auto-Sum", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pin",
                            tint = if (isPinned) AccentPurple else TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NOTE_TAGS.drop(1).forEach { t ->
                    FilterChip(
                        selected = tag == t,
                        onClick = { tag = t },
                        label = { Text(t, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Notes / Details") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (isChecklist) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "CHECKLIST ITEMS (OPTIONAL AMOUNT)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        placeholder = { Text("Item name", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = newItemAmount,
                        onValueChange = { newItemAmount = it },
                        placeholder = { Text("$currencySymbol Amt", fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.9f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    IconButton(
                        onClick = {
                            if (newItemText.isNotBlank()) {
                                checklistItems.add(
                                    NoteChecklistItem(
                                        text = newItemText.trim(),
                                        amount = newItemAmount.toDoubleOrNull() ?: 0.0,
                                        isChecked = false
                                    )
                                )
                                newItemText = ""
                                newItemAmount = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentPurple)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                checklistItems.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = { checked ->
                                    checklistItems[index] = item.copy(isChecked = checked)
                                }
                            )
                            Text(
                                text = item.text,
                                fontSize = 13.sp,
                                color = TextDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (item.amount > 0) {
                            Text(
                                text = "$currencySymbol${String.format(Locale.US, "%,.0f", item.amount)}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentPurple
                            )
                        }
                        IconButton(onClick = { checklistItems.removeAt(index) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = SoftRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (newItemText.isNotBlank() && isChecklist) {
                        checklistItems.add(
                            NoteChecklistItem(
                                text = newItemText.trim(),
                                amount = newItemAmount.toDoubleOrNull() ?: 0.0,
                                isChecked = false
                            )
                        )
                    }
                    onSave(
                        NoteEntity(
                            id = initialNote?.id ?: 0L,
                            title = title.trim().ifBlank { "Quick Note" },
                            content = content.trim(),
                            tag = tag,
                            isPinned = isPinned,
                            isChecklist = isChecklist,
                            checklistJson = encodeChecklistJson(checklistItems),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Text("Save Entry", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun parseChecklistJson(jsonStr: String): List<NoteChecklistItem> {
    return try {
        val arr = JSONArray(jsonStr)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                add(
                    NoteChecklistItem(
                        text = obj.optString("text", ""),
                        amount = obj.optDouble("amount", 0.0),
                        isChecked = obj.optBoolean("isChecked", false)
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun encodeChecklistJson(items: List<NoteChecklistItem>): String {
    val arr = JSONArray()
    items.forEach { item ->
        val obj = JSONObject().apply {
            put("text", item.text)
            put("amount", item.amount)
            put("isChecked", item.isChecked)
        }
        arr.put(obj)
    }
    return arr.toString()
}
