package com.ramble.feature.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
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
import coil.compose.AsyncImage
import com.ramble.core.ai.Expense
import com.ramble.core.ai.ProfileRepository
import com.ramble.core.designsystem.theme.BrandLightGreen
import com.ramble.core.designsystem.theme.DeepGraphite
import com.ramble.core.designsystem.theme.UIBackgroundGray
import com.ramble.core.designsystem.theme.UIBorderGray
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
    expenses: List<Expense>
) {
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
            MemberBalanceItem(balance = balance, isMainMember = balance.name == mainMember)
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
                SettlementItem(debt = debt)
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
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun MemberBalanceItem(balance: MemberBalance, isMainMember: Boolean) {
    val profile by ProfileRepository.profile.collectAsState()
    
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
                    if (isMainMember && profile.avatarUrl != null) {
                        AsyncImage(
                            model = profile.avatarUrl,
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
                    style = MaterialTheme.typography.bodyLarge,
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
                    style = MaterialTheme.typography.bodyLarge,
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
fun SettlementItem(debt: Debt) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = UIBackgroundGray.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                debt.debtor,
                fontWeight = FontWeight.Bold,
                color = DeepGraphite,
                style = MaterialTheme.typography.bodyMedium
            )
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.padding(horizontal = 8.dp).size(16.dp),
                tint = Color.Gray
            )
            Text(
                debt.creditor,
                fontWeight = FontWeight.Bold,
                color = DeepGraphite,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.weight(1f))
            Text(
                java.text.NumberFormat.getCurrencyInstance(Locale.US).format(debt.amount),
                fontWeight = FontWeight.Bold,
                color = DeepGraphite,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
