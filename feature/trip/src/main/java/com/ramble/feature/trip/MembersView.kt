package com.ramble.feature.trip

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ramble.core.ai.Expense
import com.ramble.core.designsystem.theme.BrandLightGreen
import com.ramble.core.designsystem.theme.DeepGraphite
import com.ramble.core.designsystem.theme.UIBackgroundGray
import com.ramble.core.designsystem.theme.UIBorderGray
import com.ramble.core.designsystem.util.shimmer
import java.util.Locale

data class MemberBalance(
    val name: String,
    val balance: Double
)

data class Debt(
    val debtor: String,
    val creditor: String,
    val amount: Double
)

fun calculateBalances(members: List<String>, expenses: List<Expense>): List<MemberBalance> {
    val totalPaid = mutableMapOf<String, Double>()
    val totalOwed = mutableMapOf<String, Double>()

    // Ensure all members are accounted for
    val allMembers = (members + expenses.map { it.payer } + expenses.flatMap { it.participants }).distinct()
    allMembers.forEach { 
        totalPaid[it] = 0.0
        totalOwed[it] = 0.0
    }

    expenses.forEach { expense ->
        totalPaid[expense.payer] = (totalPaid[expense.payer] ?: 0.0) + expense.amount
        if (expense.participants.isNotEmpty()) {
            val share = expense.amount / expense.participants.size
            expense.participants.forEach { participant ->
                totalOwed[participant] = (totalOwed[participant] ?: 0.0) + share
            }
        }
    }

    return allMembers.map { name ->
        MemberBalance(name, (totalPaid[name] ?: 0.0) - (totalOwed[name] ?: 0.0))
    }
}

fun calculateSettlements(balances: List<MemberBalance>): List<Debt> {
    val debts = mutableListOf<Debt>()
    val creditors = balances.filter { it.balance > 0.01 }
        .sortedByDescending { it.balance }
        .map { it.name to it.balance }
        .toMutableList()
    val debtors = balances.filter { it.balance < -0.01 }
        .sortedBy { it.balance }
        .map { it.name to -it.balance }
        .toMutableList()

    var i = 0
    var j = 0
    while (i < debtors.size && j < creditors.size) {
        val (debtorName, debtorAmount) = debtors[i]
        val (creditorName, creditorAmount) = creditors[j]
        val settled = minOf(debtorAmount, creditorAmount)

        if (settled > 0.01) {
            debts.add(Debt(debtorName, creditorName, settled))
        }

        debtors[i] = debtorName to debtorAmount - settled
        creditors[j] = creditorName to creditorAmount - settled

        if (debtors[i].second < 0.01) i++
        if (creditors[j].second < 0.01) j++
    }
    return debts
}

@Composable
fun MembersView(
    members: List<String>,
    expenses: List<Expense>,
    memberAvatarUrls: Map<String, String> = emptyMap(),
    isLoading: Boolean = false,
    onSettleDebt: (Debt) -> Unit = {}
) {
    if (isLoading) {
        MembersSkeleton()
        return
    }

    val balances = remember(members, expenses) { calculateBalances(members, expenses) }
    val mainMember = remember(members) { members.firstOrNull() ?: "Me" }
    val myBalance = remember(balances, mainMember) { balances.find { it.name == mainMember }?.balance ?: 0.0 }
    val settlements = remember(balances) { calculateSettlements(balances) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SummaryCard(myBalance = myBalance)
        }

        item {
            Text(
                "Members",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DeepGraphite,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(balances.sortedBy { it.name != mainMember }) { balance ->
            MemberBalanceItem(
                balance = balance,
                isMainMember = balance.name == mainMember,
                avatarUrl = memberAvatarUrls[balance.name]
            )
        }

        if (settlements.isNotEmpty()) {
            item {
                Text(
                    "Suggested Settlements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepGraphite,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            items(settlements) { debt ->
                SettlementItem(
                    debt = debt,
                    onSettleClick = { onSettleDebt(debt) }
                )
            }
        }
    }
}

@Composable
fun SummaryCard(myBalance: Double) {
    val containerColor = when {
        myBalance > 0.01 -> BrandLightGreen
        myBalance < -0.01 -> Color(0xFFFFEBEE)
        else -> DeepGraphite
    }
    val contentColor = if (myBalance < -0.01) Color(0xFFD32F2F) else if (myBalance > 0.01) DeepGraphite else Color.White
    val title = when {
        myBalance > 0.01 -> "You are owed"
        myBalance < -0.01 -> "You owe"
        else -> "You are all settled up"
    }
    val amount = if (myBalance.takeIf { it != 0.0 } != null) {
        java.text.NumberFormat.getCurrencyInstance(Locale.US).format(kotlin.math.abs(myBalance))
    } else ""

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = containerColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(24.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = when {
                    myBalance > 0.01 -> Icons.Default.TrendingUp
                    myBalance < -0.01 -> Icons.Default.TrendingDown
                    else -> Icons.Default.CheckCircle
                },
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = contentColor)
            if (amount.isNotEmpty()) {
                Text(
                    amount,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun MemberBalanceItem(
    balance: MemberBalance,
    isMainMember: Boolean,
    avatarUrl: String? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, UIBorderGray, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isMainMember) DeepGraphite else UIBackgroundGray),
                    contentAlignment = Alignment.Center
                ) {
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(
                            balance.name.take(1).uppercase(),
                            color = if (isMainMember) Color.White else DeepGraphite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    balance.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = DeepGraphite
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                val formatted = java.text.NumberFormat.getCurrencyInstance(Locale.US).format(kotlin.math.abs(balance.balance))
                val color = when {
                    balance.balance > 0.01 -> BrandLightGreen
                    balance.balance < -0.01 -> Color(0xFFD32F2F)
                    else -> Color.Gray
                }
                Text(
                    text = if (balance.balance > 0.01) "+ $formatted" else if (balance.balance < -0.01) "- $formatted" else "$ 0",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = if (balance.balance > 0.01) "is owed" else if (balance.balance < -0.01) "owes" else "settled",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun SettlementItem(
    debt: Debt,
    onSettleClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, UIBorderGray.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ower",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = debt.debtor,
                        fontWeight = FontWeight.Bold,
                        color = DeepGraphite,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.padding(horizontal = 12.dp).size(20.dp),
                    tint = BrandLightGreen
                )

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Recipient",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = debt.creditor,
                        fontWeight = FontWeight.Bold,
                        color = DeepGraphite,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = UIBackgroundGray.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Amount",
                            style = MaterialTheme.typography.labelMedium,
                            color = DeepGraphite.copy(alpha = 0.7f)
                        )
                        Text(
                            text = java.text.NumberFormat.getCurrencyInstance(Locale.US).format(debt.amount),
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepGraphite,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Button(
                    onClick = onSettleClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandLightGreen),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = DeepGraphite
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Settle",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = DeepGraphite
                    )
                }
            }
        }
    }
}

@Composable
fun MembersSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .shimmer()
            )
        }

        item {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(100.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmer()
            )
        }

        items(5) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .border(1.dp, UIBorderGray, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .shimmer()
                        )
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmer()
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmer()
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .shimmer()
                        )
                    }
                }
            }
        }
    }
}
