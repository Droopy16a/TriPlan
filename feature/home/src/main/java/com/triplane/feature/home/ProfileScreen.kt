package com.triplane.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.triplane.core.designsystem.theme.*

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.White
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp) // Space for bottom nav
        ) {
            item {
                ProfileHeader()
            }

            item {
                HighlightCards()
            }

            item {
                SectionHeader(title = "My travel profile")
                TravelProfileList()
            }

//            item {
//                SectionHeader(title = "🌍 Travel history")
//                TravelHistorySection()
//            }

            item {
                SectionHeader(title = "Favorites")
                FavoritesSection()
            }

            item {
                SectionHeader(title = "My trips")
                MyTripsSection()
            }

            item {
                SectionHeader(title = "AI preferences")
                AiPreferencesSection()
            }

            item {
                SectionHeader(title = "Account & app settings")
                SettingsSection()
            }
            
            item {
                BusinessBanner()
            }
            
            item {
                VersionInfo()
            }
        }
    }
}

@Composable
fun ProfileHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Profile Photo
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(UIBackgroundGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = DeepGraphite.copy(alpha = 0.3f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Alex Martin",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = DeepGraphite
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(count = "12", label = "trips")
            VerticalDivider()
            StatItem(count = "8", label = "destinations")
            VerticalDivider()
            StatItem(count = "€2,430", label = "tracked")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Badges and profile pill
        Surface(
            shape = RoundedCornerShape(50),
            color = Color.White,
            border = BorderStroke(1.dp, UIBorderGray.copy(alpha = 0.6f)),
            modifier = Modifier.clickable { }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    repeat(3) { i ->
                        Icon(
                            imageVector = when(i) {
                                0 -> Icons.Default.AutoAwesome
                                1 -> Icons.Default.Favorite
                                else -> Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = when(i) {
                                0 -> Color(0xFF3A86FF)
                                1 -> Color(0xFFE63946)
                                else -> Color(0xFF2E7D32)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Badges and profile",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun VerticalDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .width(1.dp)
            .height(30.dp)
            .background(UIBorderGray)
    )
}

@Composable
fun StatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(text = count, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DeepGraphite)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
fun HighlightCards() {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 24.dp, vertical = 8.dp),
//        horizontalArrangement = Arrangement.spacedBy(16.dp)
//    ) {
//        HighlightCard(
//            modifier = Modifier.weight(1f),
//            topText = "€ 250",
//            topTextColor = Color(0xFF3A86FF),
//            title = "Saved this month",
//            subtitle = "TRIPLANE25"
//        )
//        HighlightCard(
//            modifier = Modifier.weight(1f),
//            topText = "+1200 pts",
//            topTextColor = Color(0xFFFFA303),
//            title = "Refer a friend",
//            subtitle = "Share your code"
//        )
//    }
}

@Composable
fun HighlightCard(
    modifier: Modifier = Modifier,
    topText: String,
    topTextColor: Color,
    title: String,
    subtitle: String
) {
    Surface(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, UIBorderGray.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                color = topTextColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = topText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = topTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
    )
}

@Composable
fun TravelProfileList() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        ProfileListItem(Icons.Default.Flight, "Travel style", badgeText = "Balanced")
        ProfileListItem(Icons.Default.Explore, "Interests", badgeCount = 3)
        ProfileListItem(Icons.Default.Hotel, "Accommodation")
        ProfileListItem(Icons.Default.DirectionsBus, "Transportation")
        ProfileListItem(Icons.Default.Restaurant, "Food preferences")
    }
}

@Composable
fun ProfileListItem(
    icon: ImageVector,
    label: String,
    badgeText: String? = null,
    badgeCount: Int? = null,
    color: Color = DeepGraphite
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = color.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color)
        Spacer(modifier = Modifier.weight(1f))
        
        if (badgeText != null) {
            Surface(
                color = Color(0xFFFFF3E0),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = badgeText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Color(0xFFE65100),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        if (badgeCount != null) {
            Surface(
                color = DeepGraphite,
                shape = CircleShape,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = badgeCount.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
    }
}



@Composable
fun TravelHistorySection() {
    Surface(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF8F9FA),
        border = BorderStroke(1.dp, UIBorderGray.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            HistoryItem("🇫🇷 France", "4 trips")
            HistoryItem("🇮🇹 Italy", "2 trips")
            HistoryItem("🇯🇵 Japan", "1 trip")
            HistoryItem("🇪🇸 Spain", "2 trips")
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Map coming soon...",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun HistoryItem(country: String, count: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(country, fontWeight = FontWeight.Medium)
        Text(count, color = Color.Gray)
    }
}

@Composable
fun FavoritesSection() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        ProfileListItem(Icons.Default.Place, "Favorite destinations")
        ProfileListItem(Icons.Default.Bookmark, "Saved places")
        ProfileListItem(Icons.Default.Flight, "Favorite trips")
        ProfileListItem(Icons.Default.Hotel, "Saved hotels/accommodations")
        ProfileListItem(Icons.Default.Restaurant, "Saved restaurants")
    }
}

@Composable
fun MyTripsSection() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("Upcoming", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Normal)
        TripSummaryItem("🍝 Rome 2026", "Sep 12–17")
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Past", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Normal)
        TripSummaryItem("🗼 Paris 2026", "Aug 2–6")
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepGraphite)
        ) {
            Text("View all trips")
        }
    }
}

@Composable
fun TripSummaryItem(title: String, dates: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(UIBackgroundGray),
            contentAlignment = Alignment.Center
        ) {
            Text(title.take(2), fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title.drop(3), fontWeight = FontWeight.Bold)
            Text(dates, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun AiPreferencesSection() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            "How should TripLane plan for me?",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        FlowRow(spacing = 8.dp) {
            Chip("⚡ Pack my days")
            Chip("🌿 Relaxed pace", selected = true)
            Chip("🚶 Prefer walking", selected = true)
            Chip("🚇 Prefer public transport")
            Chip("🍽️ Prioritize food", selected = true)
            Chip("🏛️ Prioritize culture")
            Chip("💰 Prioritize saving money")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "AI planning behavior",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        AiBehaviorToggle("Avoid overly touristy places", checked = true)
        AiBehaviorToggle("Include local restaurants", checked = true)
        AiBehaviorToggle("Leave free time between activities", checked = true)
    }
}

@Composable
fun AiBehaviorToggle(label: String, checked: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { },
            colors = CheckboxDefaults.colors(checkedColor = DeepGraphite)
        )
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SettingsSection() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        ProfileListItem(Icons.Default.Person, "Edit profile")
        ProfileListItem(Icons.Default.Lock, "Email & password")
        ProfileListItem(Icons.Default.Link, "Connected accounts")
        ProfileListItem(Icons.Default.Notifications, "Notifications")
        ProfileListItem(Icons.Default.Language, "Language")
        ProfileListItem(Icons.Default.CurrencyExchange, "Currency")
        ProfileListItem(Icons.Default.Straighten, "Units (km / miles)")
        ProfileListItem(Icons.Default.Palette, "Theme")
        ProfileListItem(Icons.Default.PrivacyTip, "Privacy")
        ProfileListItem(Icons.Default.Description, "Terms")
        ProfileListItem(Icons.Default.HelpOutline, "Help & support")
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Log out",
            modifier = Modifier
                .clickable { }
                .padding(vertical = 12.dp),
            color = TravelRed,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            "Delete account",
            modifier = Modifier
                .clickable { }
                .padding(vertical = 12.dp),
            color = TravelRed,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun BusinessBanner() {
    Surface(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, UIBorderGray.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = SkyBlueLight.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Business, contentDescription = null, tint = SkyBlue)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("TripLane for Business", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "List your properties or services on TripLane and reach more travelers.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@Composable
fun VersionInfo() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "v1.0.42",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun Chip(label: String, selected: Boolean = false) {
    Surface(
        color = if (selected) DeepGraphite else Color.White,
        shape = RoundedCornerShape(50),
        border = if (selected) null else BorderStroke(1.dp, UIBorderGray),
        modifier = Modifier.clickable { }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) Color.White else DeepGraphite,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun FlowRow(
    spacing: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(content = content) { measurables, constraints ->
        val xSpace = spacing.roundToPx()
        val ySpace = spacing.roundToPx()
        val placeables = measurables.map { it.measure(constraints) }
        
        var currentX = 0
        var currentY = 0
        var rowHeight = 0
        
        val layoutWidth = constraints.maxWidth
        val positions = mutableListOf<Pair<Int, Int>>()
        
        placeables.forEach { placeable ->
            if (currentX + placeable.width > layoutWidth) {
                currentX = 0
                currentY += rowHeight + ySpace
                rowHeight = 0
            }
            positions.add(currentX to currentY)
            rowHeight = maxOf(rowHeight, placeable.height)
            currentX += placeable.width + xSpace
        }
        
        layout(layoutWidth, currentY + rowHeight) {
            placeables.forEachIndexed { index, placeable ->
                val (x, y) = positions[index]
                placeable.placeRelative(x, y)
            }
        }
    }
}
