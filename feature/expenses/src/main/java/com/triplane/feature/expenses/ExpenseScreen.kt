package com.triplane.feature.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.triplane.core.designsystem.theme.DeepGraphite
import com.triplane.core.designsystem.theme.BrandLightGreen
import com.triplane.core.designsystem.theme.UIBackgroundGray
import com.triplane.core.designsystem.theme.UIBorderGray
import com.triplane.core.designsystem.util.clickWithDelay

@Composable
fun ExpenseScreen(totalBudget: Double = 2500.0, spentBudget: Double = 1200.0) {
    val scope = rememberCoroutineScope()
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
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "October 12, 2026",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepGraphite,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            item {
                FlatExpenseItem(
                    emoji = "💳",
                    title = "Refund",
                    subtitle = "Transferred by Quentin"
                )
            }
            
            item {
                FlatExpenseItem(
                    emoji = "🎁",
                    title = "Gift for Augustin",
                    subtitle = "Transferred by Gaetan (me)"
                )
            }
            
            item {
                FlatExpenseItem(
                    emoji = "💳",
                    title = "Refund",
                    subtitle = "Transferred by Jules"
                )
            }
        }

        // FAB
        FloatingActionButton(
            onClick = {
                clickWithDelay(scope) {
                    /* TODO: Add Expense */
                }
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

@Composable
fun BudgetCard(totalBudget: Double = 2500.0, spentBudget: Double = 1200.0) {
    val remainingBudget = (totalBudget - spentBudget).coerceAtLeast(0.0)
    val progress = if (totalBudget > 0) (spentBudget / totalBudget).toFloat().coerceIn(0f, 1f) else 0f

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
            val formatBudget = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US).apply { 
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

@Composable
fun FlatExpenseItem(emoji: String, title: String, subtitle: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            .border(1.dp, UIBorderGray, RoundedCornerShape(20.dp)),
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
}
