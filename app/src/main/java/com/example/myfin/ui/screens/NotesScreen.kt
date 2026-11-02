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
import androidx.compose.ui.graphics.Color
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

    Scaffold(
        containerColor = CanvasLight,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingNote = null
                    showEditorSheet = true
                },
                containerColor = AccentPurple,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Note")
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Note", fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            // 1. TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(38.dp)
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
                            text = "Notes & Checklists",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = TextDark,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = "Financial Memos, Shopping Lists & IOUs",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CardWhite)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Back",
                        tint = TextDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 2. SEARCH BAR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search memos, tags, or checklists...", fontSize = 12.5.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardWhite,
                    unfocusedContainerColor = CardWhite,
                    unfocusedBorderColor = BorderLight,
                    focusedBorderColor = AccentPurple
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. TAG FILTER PILLS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NOTE_TAGS.forEach { tag ->
                    val isSelected = selectedTag == tag
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) AccentPurple else CardWhite,
                        border = BorderStroke(0.8.dp, if (isSelected) AccentPurple else BorderLight),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedTag = tag }
                    ) {
                        Text(
                            text = tag,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else TextMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. NOTES LIST
            if (filteredNotes.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = CardWhite,
                    border = BorderStroke(0.8.dp, BorderLight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = AccentPurple.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No financial notes found",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap '+ New Note' to create a memo or an auto-summing financial checklist.",
                            fontSize = 11.5.sp,
                            color = TextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        NoteCardItem(
                            note = note,
                            currencySymbol = currencySymbol,
                            onClick = {
                                editingNote = note
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
            width = if (note.isPinned) 1.dp else 0.8.dp,
            color = if (note.isPinned) AccentPurple.copy(alpha = 0.55f) else BorderLight.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Title + Tag + Pin/Delete
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
                            tint = AccentPurple,
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
                        color = AccentPurple.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = note.tag,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple,
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
                            tint = if (note.isPinned) AccentPurple else TextMuted.copy(alpha = 0.5f),
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
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (note.isChecklist && checklistItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
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
                            color = AccentPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Timestamp + Auto-Sum Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormatted,
                    fontSize = 10.sp,
                    color = TextMuted
                )

                if (note.isChecklist && totalAmount > 0.0) {
                    Text(
                        text = "Pending: $currencySymbol${String.format(Locale.US, "%,.0f", pendingAmount)} / Total: $currencySymbol${String.format(Locale.US, "%,.0f", totalAmount)}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (pendingAmount > 0) SoftAmber else SoftGreen
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorModalSheet(
    initialNote: NoteEntity?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (NoteEntity) -> Unit
) {
    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var content by remember { mutableStateOf(initialNote?.content ?: "") }
    var tag by remember { mutableStateOf(initialNote?.tag ?: "General") }
    var isPinned by remember { mutableStateOf(initialNote?.isPinned ?: false) }
    var isChecklist by remember { mutableStateOf(initialNote?.isChecklist ?: false) }

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
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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
                    text = if (initialNote == null) "Create Financial Note" else "Edit Note",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = TextDark
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = isChecklist,
                        onClick = { isChecklist = !isChecklist },
                        label = { Text("Checklist + Auto-Sum", fontSize = 11.sp) }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
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

            // Tag selector
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
                label = { Text("Memo / Notes") },
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
                Text("Save Note", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
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
