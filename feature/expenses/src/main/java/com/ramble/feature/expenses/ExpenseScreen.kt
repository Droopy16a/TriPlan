package com.ramble.feature.expenses

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.ramble.core.designsystem.theme.DeepGraphite
import com.ramble.core.designsystem.theme.BrandLightGreen
import com.ramble.core.designsystem.theme.UIBackgroundGray
import com.ramble.core.designsystem.theme.UIBorderGray

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.ramble.core.ai.Expense
import java.util.UUID

@Composable
fun ExpenseScreen(
    totalBudget: Double = 2500.0,
    spentBudget: Double = 1200.0,
    expenses: List<Expense> = emptyList(),
    memberNames: List<String> = listOf("Me", "member 1", "member 2", "member 3"),
    prefilledTitle: String? = null,
    prefilledAmount: Double? = null,
    onPrefillHandled: () -> Unit = {},
    onAddExpense: (Expense) -> Unit = {},
    onUpdateExpense: (Expense) -> Unit = {},
    onDeleteExpense: (String) -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Expense?>(null) }
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }

    LaunchedEffect(prefilledTitle, prefilledAmount) {
        if (prefilledTitle != null || prefilledAmount != null) {
            showAddDialog = true
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(
            participants = memberNames,
            initialTitle = prefilledTitle ?: "",
            initialAmount = prefilledAmount?.toString() ?: "",
            onDismiss = { 
                showAddDialog = false
                if (prefilledTitle != null || prefilledAmount != null) onPrefillHandled()
            },
            onConfirm = { expense ->
                onAddExpense(expense)
                showAddDialog = false
                if (prefilledTitle != null || prefilledAmount != null) onPrefillHandled()
            }
        )
    }

    if (showEditDialog != null) {
        AddExpenseDialog(
            participants = memberNames,
            expenseToEdit = showEditDialog,
            onDismiss = { showEditDialog = null },
            onConfirm = { updated ->
                onUpdateExpense(updated)
                showEditDialog = null
            }
        )
    }

    selectedExpense?.let { expense ->
        ExpenseDetailDialog(
            expense = expense,
            onDismiss = { selectedExpense = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BudgetCard(totalBudget = totalBudget, spentBudget = spentBudget)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(DeepGraphite))
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(UIBackgroundGray))
                }
            }

            if (expenses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No expenses yet. Tap + to add one!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                // Simple grouping by date (assuming ISO format or just "Oct 12, 2026")
                val grouped = expenses.groupBy { it.date }
                grouped.forEach { (date, items) ->
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = date,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepGraphite,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items.forEach { expense ->
                        item {
                            FlatExpenseItem(
                                emoji = expense.emoji,
                                title = expense.title,
                                subtitle = "${expense.subtitle} · $ ${expense.amount}",
                                onClick = { selectedExpense = expense },
                                onEdit = { showEditDialog = expense },
                                onDelete = { onDeleteExpense(expense.id) }
                            )
                        }
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = {
                showAddDialog = true
            },
            containerColor = DeepGraphite,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
                .size(64.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Expense", modifier = Modifier.size(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    expenseToEdit: Expense? = null,
    participants: List<String> = listOf("Me", "member 1", "member 2", "member 3"),
    initialTitle: String = "",
    initialAmount: String = "",
    onDismiss: () -> Unit,
    onConfirm: (Expense) -> Unit
) {
    var title by remember { mutableStateOf(expenseToEdit?.title ?: initialTitle) }
    var amount by remember { mutableStateOf(expenseToEdit?.amount?.toString() ?: initialAmount) }
    var paidBy by remember { mutableStateOf(expenseToEdit?.payer ?: participants.firstOrNull() ?: "Me") }
    var date by remember {
        mutableStateOf(
            if (expenseToEdit != null) {
                try {
                    java.time.LocalDate.parse(expenseToEdit.date, DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US))
                } catch (_: Exception) {
                    java.time.LocalDate.now()
                }
            } else {
                java.time.LocalDate.now()
            }
        )
    }
    var isShared by remember { mutableStateOf(expenseToEdit?.participants?.isNotEmpty() ?: false) }
    var emoji by remember { mutableStateOf(expenseToEdit?.emoji ?: "💳") }

    val selectedParticipants = remember { 
        mutableStateListOf<String>().apply {
            expenseToEdit?.participants?.let { addAll(it) }
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    var expandedPaidBy by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK", color = DeepGraphite) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = DeepGraphite) }
            },
            colors = DatePickerDefaults.colors(containerColor = Color.White)
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "Select date",
                        modifier = Modifier.padding(start = 24.dp, top = 24.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = DeepGraphite.copy(alpha = 0.6f)
                    )
                },
                headline = {
                    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
                    val dateText = datePickerState.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
                    } ?: "No date"

                    Text(
                        text = dateText,
                        modifier = Modifier.padding(start = 24.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = DeepGraphite
                    )
                },
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = DeepGraphite,
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = DeepGraphite,
                    todayContentColor = DeepGraphite,
                    containerColor = Color.White
                ),
                showModeToggle = false
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        if (expenseToEdit != null) "Edit Expense" else "Add Expense",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DeepGraphite
                    )
                    Text(
                        if (expenseToEdit != null) "Update your spending info" else "Track your spending manually",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepGraphite.copy(alpha = 0.5f)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = DeepGraphite.copy(alpha = 0.6f))
                }
            }
        },
        text = {
            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DeepGraphite,
                focusedLabelColor = DeepGraphite,
                cursorColor = DeepGraphite,
                unfocusedBorderColor = Color(0xFFDDDDDD),
                unfocusedLabelColor = Color(0xFF888888),
                selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
                    handleColor = DeepGraphite,
                    backgroundColor = DeepGraphite.copy(alpha = 0.4f)
                )
            )
            val emojis = listOf("💳", "🍽️", "🚕", "🏨", "✈️", "🎁", "🛍️", "☕", "🎟️", "🍺")

            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text("Category", style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        items(emojis.size) { index ->
                            val e = emojis[index]
                            val isSelected = emoji == e
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) DeepGraphite else UIBackgroundGray)
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) DeepGraphite else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { emoji = e },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(e, fontSize = 22.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g. Dinner at Kyoto") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = textFieldColors
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount ($)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = textFieldColors
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Paid By Selection
                    ExposedDropdownMenuBox(
                        expanded = expandedPaidBy,
                        onExpandedChange = { expandedPaidBy = !expandedPaidBy },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = paidBy,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Paid By") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPaidBy) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = textFieldColors
                        )
                        ExposedDropdownMenu(
                            expanded = expandedPaidBy,
                            onDismissRequest = { expandedPaidBy = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            participants.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        paidBy = selectionOption
                                        expandedPaidBy = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                        onValueChange = { },
                        label = { Text("Date") },
                        readOnly = true,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker = true },
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = DeepGraphite,
                            disabledBorderColor = Color(0xFFDDDDDD),
                            disabledLabelColor = Color(0xFF888888),
                            disabledLeadingIconColor = DeepGraphite.copy(alpha = 0.6f)
                        )
                    )
                }

                // Share Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isShared,
                            onCheckedChange = { isShared = it },
                            colors = CheckboxDefaults.colors(checkedColor = DeepGraphite)
                        )
                        Text("Share this expense", style = MaterialTheme.typography.bodyMedium, color = DeepGraphite)
                    }

                    if (isShared) {
                        participants.forEach { participant ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 32.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedParticipants.contains(participant),
                                    onCheckedChange = {
                                        if (it) selectedParticipants.add(participant)
                                        else selectedParticipants.remove(participant)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = DeepGraphite)
                                )
                                Text(participant, style = MaterialTheme.typography.bodySmall, color = DeepGraphite)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val isFormValid = title.isNotBlank() && 
                            amount.isNotBlank() && 
                            (amount.replace(",", ".").toDoubleOrNull() ?: 0.0) > 0.0 &&
                            paidBy.isNotBlank() &&
                            (!isShared || selectedParticipants.isNotEmpty())

            Button(
                onClick = {
                    val sanitizedAmount = amount.replace(",", ".")
                    val amountDouble = sanitizedAmount.toDoubleOrNull() ?: 0.0
                    onConfirm(
                        Expense(
                            id = expenseToEdit?.id ?: UUID.randomUUID().toString(),
                            emoji = emoji,
                            title = title,
                            subtitle = if (isShared) "Shared with ${selectedParticipants.size} people" else "Paid by $paidBy",
                            amount = amountDouble,
                            date = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US).format(date),
                            payer = paidBy,
                            participants = if (isShared) selectedParticipants.toList() else listOf(paidBy),
                            isSettlement = expenseToEdit?.isSettlement ?: false
                        )
                    )
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepGraphite,
                    disabledContainerColor = DeepGraphite.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    if (expenseToEdit != null) "Update Expense" else "Add Expense",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                    color = if (isFormValid) Color.White else Color.White.copy(alpha = 0.6f)
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun BudgetCard(totalBudget: Double = 2500.0, spentBudget: Double = 1200.0) {
    val remainingBudget = (totalBudget - spentBudget).coerceAtLeast(0.0)
    val targetProgress = if (totalBudget > 0) (spentBudget / totalBudget).toFloat().coerceIn(0f, 1f) else 0f
    
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "budgetProgress"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.2f))
            .border(1.dp, UIBorderGray, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp)
        ) {
            Text("Total Budget", style = MaterialTheme.typography.titleSmall, color = DeepGraphite)
            Spacer(modifier = Modifier.height(8.dp))
            val formatBudget = java.text.NumberFormat.getCurrencyInstance(Locale.US).apply {
                maximumFractionDigits = 0
            }.format(totalBudget)
            Text(formatBudget, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = DeepGraphite)

            Spacer(modifier = Modifier.height(24.dp))

            val currencyFormatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US).apply {
                maximumFractionDigits = 0
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(currencyFormatter.format(spentBudget), style = MaterialTheme.typography.bodyMedium, color = DeepGraphite, fontWeight = FontWeight.Medium)
                Text(currencyFormatter.format(remainingBudget), style = MaterialTheme.typography.bodyMedium, color = DeepGraphite, fontWeight = FontWeight.Medium)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(UIBackgroundGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50))
                        .background(BrandLightGreen)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FlatExpenseItem(
    emoji: String, 
    title: String, 
    subtitle: String, 
    onClick: () -> Unit = {}, 
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.05f))
                .border(1.dp, UIBorderGray, RoundedCornerShape(20.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                ),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DeepGraphite),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = DeepGraphite)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle, 
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepGraphite.copy(alpha = 0.7f)
                )
            }
        }
    }

        Box(modifier = Modifier.align(Alignment.TopEnd).padding(end = 16.dp)) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color.White)
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = { 
                        showMenu = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = Color.Red) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailDialog(
    expense: Expense,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Expense Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = DeepGraphite
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = DeepGraphite.copy(alpha = 0.6f))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header info
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DeepGraphite),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(expense.emoji, fontSize = 40.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(expense.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepGraphite)
                    Text(expense.date, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }

                // Paid By section
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Paid By", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = DeepGraphite)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = UIBackgroundGray.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(DeepGraphite),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(expense.payer.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(expense.payer, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = DeepGraphite)
                            }
                            Text(
                                text = String.format(Locale.US, "$ %.2f", expense.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepGraphite
                            )
                        }
                    }
                }

                // Participants section
                if (expense.participants.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Participants", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = DeepGraphite)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = UIBackgroundGray.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                val shareAmount = expense.amount / expense.participants.size
                                expense.participants.forEach { participant ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(participant.take(1).uppercase(), color = DeepGraphite, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(participant, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = DeepGraphite)
                                            }
                                        }
                                        Text(
                                            text = String.format(Locale.US, "$ %.2f", shareAmount),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = DeepGraphite
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepGraphite)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp)
    )
}
