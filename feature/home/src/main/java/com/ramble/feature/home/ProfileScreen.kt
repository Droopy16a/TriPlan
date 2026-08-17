package com.ramble.feature.home

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.ramble.core.ai.ProfileRepository
import com.ramble.core.ai.UserProfile
import com.ramble.core.auth.AuthRepository
import com.ramble.core.designsystem.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class ProfileCategory {
    TRAVEL_STYLE, INTERESTS, ACCOMMODATION, TRANSPORTATION, FOOD,
    EDIT_PROFILE, NOTIFICATIONS, LANGUAGE, CURRENCY, UNITS, THEME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val profile by ProfileRepository.profile.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<ProfileCategory?>(null) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.White
    ) {
        val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = topPadding + 24.dp,
                bottom = bottomPadding + 100.dp
            )
        ) {
            item {
                ProfileHeader(profile.name, profile.avatarUrl)
            }

            item {
                HighlightCards()
            }

            item {
                SectionHeader(title = "My travel profile")
                TravelProfileList(
                    profile = profile,
                    onCategoryClick = { category ->
                        selectedCategory = category
                        showSheet = true
                    }
                )
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

//            item {
//                SectionHeader(title = "AI preferences")
//                AiPreferencesSection()
//            }

            item {
                SectionHeader(title = "Account & app settings")
                SettingsSection(
                    profile = profile,
                    onCategoryClick = { category ->
                        selectedCategory = category
                        showSheet = true
                    },
                    onSignOut = onSignOut
                )
            }

//            item {
//                BusinessBanner()
//            }

            item {
                VersionInfo()
            }
        }
    }

    if (showSheet && selectedCategory != null) {
        Dialog(
            onDismissRequest = { showSheet = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showSheet = false }
                    ),
                contentAlignment = Alignment.Center
            ) {
                val isEditProfile = selectedCategory == ProfileCategory.EDIT_PROFILE
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(if (isEditProfile) 0.95f else 0.9f)
                        .then(if (isEditProfile) Modifier.fillMaxHeight(0.9f) else Modifier)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    ProfileSelectionContent(
                        category = selectedCategory!!,
                        profile = profile,
                        onDismiss = { showSheet = false }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(name: String, avatarUrl: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Profile Photo
        Box(
            modifier = Modifier
                .size(100.dp)
                .shadow(15.dp, CircleShape, clip = false, spotColor = Color.Black.copy(alpha = 0.28f))
                .clip(CircleShape)
                .background(UIBackgroundGray),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = DeepGraphite.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = name,
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
//        Surface(
//            shape = RoundedCornerShape(50),
//            color = Color.White,
//            border = BorderStroke(1.dp, UIBorderGray.copy(alpha = 0.6f)),
//            modifier = Modifier.clickable { }
//        ) {
//            Row(
//                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Row {
//                    repeat(3) { i ->
//                        Icon(
//                            imageVector = when(i) {
//                                0 -> Icons.Default.AutoAwesome
//                                1 -> Icons.Default.Favorite
//                                else -> Icons.Default.CheckCircle
//                            },
//                            contentDescription = null,
//                            modifier = Modifier.size(16.dp),
//                            tint = when(i) {
//                                0 -> Color(0xFF3A86FF)
//                                1 -> Color(0xFFE63946)
//                                else -> Color(0xFF2E7D32)
//                            }
//                        )
//                    }
//                }
//                Spacer(modifier = Modifier.width(8.dp))
//                Text(
//                    "Badges and profile",
//                    style = MaterialTheme.typography.bodyMedium,
//                    fontWeight = FontWeight.SemiBold
//                )
//                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
//            }
//        }
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
//            subtitle = "RAMBLE25"
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
fun TravelProfileList(
    profile: UserProfile,
    onCategoryClick: (ProfileCategory) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        ProfileListItem(
            icon = Icons.Default.Flight,
            label = "Travel style",
            badgeText = profile.travelStyle,
            onClick = { onCategoryClick(ProfileCategory.TRAVEL_STYLE) }
        )
        ProfileListItem(
            icon = Icons.Default.Explore,
            label = "Interests",
            badgeCount = if (profile.interests.isNotEmpty()) profile.interests.size else null,
            onClick = { onCategoryClick(ProfileCategory.INTERESTS) }
        )
        ProfileListItem(
            icon = Icons.Default.Hotel,
            label = "Accommodation",
            badgeCount = if (profile.accommodationPreference.isNotEmpty()) profile.accommodationPreference.size else null,
            onClick = { onCategoryClick(ProfileCategory.ACCOMMODATION) }
        )
        ProfileListItem(
            icon = Icons.Default.DirectionsBus,
            label = "Transportation",
            badgeCount = if (profile.transportationPreference.isNotEmpty()) profile.transportationPreference.size else null,
            onClick = { onCategoryClick(ProfileCategory.TRANSPORTATION) }
        )
        ProfileListItem(
            icon = Icons.Default.Restaurant,
            label = "Food preferences",
            badgeCount = if (profile.foodPreferences.isNotEmpty()) profile.foodPreferences.size else null,
            onClick = { onCategoryClick(ProfileCategory.FOOD) }
        )
    }
}

@Composable
fun ProfileListItem(
    icon: ImageVector,
    label: String,
    badgeText: String? = null,
    badgeCount: Int? = null,
    color: Color = DeepGraphite,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
        ProfileListItem(Icons.Default.Hotel, "Saved accommodations")
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
            "How should Ramble plan for me?",
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
fun SettingsSection(
    profile: UserProfile,
    onCategoryClick: (ProfileCategory) -> Unit,
    onSignOut: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        ProfileListItem(
            icon = Icons.Default.Person,
            label = "Edit profile",
            onClick = { onCategoryClick(ProfileCategory.EDIT_PROFILE) }
        )
        ProfileListItem(
            icon = Icons.Default.Notifications,
            label = "Notifications",
            badgeCount = if (profile.unreadNotificationCount > 0) profile.unreadNotificationCount else null,
            onClick = { onCategoryClick(ProfileCategory.NOTIFICATIONS) }
        )
        ProfileListItem(
            icon = Icons.Default.Language,
            label = "Language",
            badgeText = profile.language,
            onClick = { onCategoryClick(ProfileCategory.LANGUAGE) }
        )
        ProfileListItem(
            icon = Icons.Default.CurrencyExchange,
            label = "Currency",
            badgeText = profile.currency,
            onClick = { onCategoryClick(ProfileCategory.CURRENCY) }
        )
        ProfileListItem(
            icon = Icons.Default.Straighten,
            label = "Units",
            badgeText = profile.units,
            onClick = { onCategoryClick(ProfileCategory.UNITS) }
        )
        ProfileListItem(
            icon = Icons.Default.Palette,
            label = "Theme",
            badgeText = profile.theme,
            onClick = { onCategoryClick(ProfileCategory.THEME) }
        )
        ProfileListItem(Icons.Default.PrivacyTip, "Privacy")
        ProfileListItem(Icons.Default.Description, "Terms")
        ProfileListItem(Icons.Default.HelpOutline, "Help & support")

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Log out",
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSignOut() }
                .padding(vertical = 12.dp),
            color = TravelRed,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            "Delete account",
            modifier = Modifier
                .fillMaxWidth()
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
                Text("Ramble for Business", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "List your properties or services on Ramble and reach more travelers.",
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
fun ProfileSelectionContent(
    category: ProfileCategory,
    profile: UserProfile,
    onDismiss: () -> Unit
) {
    if (category == ProfileCategory.EDIT_PROFILE) {
        EditProfileContent(profile, onDismiss)
        return
    }

    if (category == ProfileCategory.NOTIFICATIONS) {
        NotificationsContent(profile, onDismiss)
        return
    }

    val options = when (category) {
        ProfileCategory.TRAVEL_STYLE -> listOf("Budget", "Balanced", "Comfort", "Luxury")
        ProfileCategory.INTERESTS -> listOf("🏛️ Culture", "🍜 Food", "🏖️ Beaches", "🥾 Nature", "🎨 Art", "🌃 Nightlife", "🛍️ Shopping", "⚽ Sports", "📸 Photography")
        ProfileCategory.ACCOMMODATION -> listOf("Hotel", "Hostel", "Apartment", "Guest house", "Camping")
        ProfileCategory.TRANSPORTATION -> listOf("Walking", "Public transport", "Car", "Taxi", "Bike")
        ProfileCategory.FOOD -> listOf("Vegetarian", "Vegan", "Halal", "Gluten-free")
        ProfileCategory.LANGUAGE -> listOf("English", "French", "Spanish", "German", "Japanese", "Chinese")
        ProfileCategory.CURRENCY -> listOf("EUR (€)", "USD ($)", "GBP (£)", "JPY (¥)", "CAD ($)")
        ProfileCategory.UNITS -> listOf("Metric (km)", "Imperial (miles)")
        ProfileCategory.THEME -> listOf("Light", "Dark", "System")
        else -> emptyList()
    }

    val title = when (category) {
        ProfileCategory.TRAVEL_STYLE -> "Travel style"
        ProfileCategory.INTERESTS -> "Interests"
        ProfileCategory.ACCOMMODATION -> "Accommodation preference"
        ProfileCategory.TRANSPORTATION -> "Transportation preference"
        ProfileCategory.FOOD -> "Food preferences"
        ProfileCategory.LANGUAGE -> "Language"
        ProfileCategory.CURRENCY -> "Currency"
        ProfileCategory.UNITS -> "Units"
        ProfileCategory.THEME -> "Theme"
        else -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = DeepGraphite
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            FlowRow(spacing = 8.dp) {
                options.forEach { option ->
                    val isSelected = when (category) {
                        ProfileCategory.TRAVEL_STYLE -> profile.travelStyle == option
                        ProfileCategory.INTERESTS -> profile.interests.contains(option.substringAfter(" ").trim()) || profile.interests.contains(option)
                        ProfileCategory.ACCOMMODATION -> profile.accommodationPreference.contains(option)
                        ProfileCategory.TRANSPORTATION -> profile.transportationPreference.contains(option)
                        ProfileCategory.FOOD -> profile.foodPreferences.contains(option)
                        ProfileCategory.LANGUAGE -> profile.language == option
                        ProfileCategory.CURRENCY -> profile.currency == option
                        ProfileCategory.UNITS -> profile.units == option
                        ProfileCategory.THEME -> profile.theme == option
                        else -> false
                    }

                    Chip(
                        label = option,
                        selected = isSelected,
                        onClick = {
                            when (category) {
                                ProfileCategory.TRAVEL_STYLE -> ProfileRepository.updateTravelStyle(option)
                                ProfileCategory.INTERESTS -> ProfileRepository.toggleInterest(option.substringAfter(" ").trim())
                                ProfileCategory.ACCOMMODATION -> ProfileRepository.toggleAccommodation(option)
                                ProfileCategory.TRANSPORTATION -> ProfileRepository.toggleTransportation(option)
                                ProfileCategory.FOOD -> ProfileRepository.toggleFoodPreference(option)
                                ProfileCategory.LANGUAGE -> ProfileRepository.updateLanguage(option)
                                ProfileCategory.CURRENCY -> ProfileRepository.updateCurrency(option)
                                ProfileCategory.UNITS -> ProfileRepository.updateUnits(option)
                                ProfileCategory.THEME -> ProfileRepository.updateTheme(option)
                                else -> {}
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepGraphite)
        ) {
            Text("Done")
        }
    }
}

@Composable
fun NotificationsContent(
    profile: UserProfile,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        ProfileRepository.markNotificationsAsRead()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = DeepGraphite
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            if (profile.notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No notifications yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                profile.notifications.forEach { notification ->
                    NotificationItem(notification)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepGraphite)
        ) {
            Text("Done")
        }
    }
}

@Composable
fun NotificationItem(notification: com.ramble.core.ai.Notification) {
    Surface(
        color = UIBackgroundGray.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(notification.emoji, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        notification.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = DeepGraphite
                    )
                    Text(
                        notification.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                Text(
                    notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepGraphite.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun EditProfileContent(
    profile: UserProfile,
    onDismiss: () -> Unit
) {
    var firstName by remember { mutableStateOf(profile.firstName) }
    var lastName by remember { mutableStateOf(profile.lastName) }
    var email by remember { mutableStateOf(profile.email) }
    var birthDate by remember { mutableStateOf(profile.birthDate ?: "") }
    var phoneCountryCode by remember { mutableStateOf(profile.phoneCountryCode ?: "") }
    var phoneNumber by remember { mutableStateOf(profile.phoneNumber ?: "") }
    var avatarUrl by remember(profile.avatarUrl) { mutableStateOf(profile.avatarUrl) }
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            selectedAvatarUri = it
            saveError = null
        }
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = if (birthDate.isNotEmpty()) {
            try {
                LocalDate.parse(birthDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: Exception) {
                null
            }
        } else null
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Back", modifier = Modifier.size(32.dp))
            }
            Text(
                text = "Edit profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp)) // To balance the back button
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Profile Photo with Pencil Icon
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(100.dp)
                    .shadow(16.dp, CircleShape, clip = false, spotColor = Color.Black.copy(alpha = 0.28f))
                    .clip(CircleShape)
                    .clickable(enabled = !isSaving) {
                        avatarPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(UIBackgroundGray),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarModel = selectedAvatarUri ?: avatarUrl
                    if (avatarModel != null) {
                        AsyncImage(
                            model = avatarModel,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = DeepGraphite.copy(alpha = 0.3f)
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(36.dp)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .clickable(enabled = !isSaving) {
                            avatarPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    shape = CircleShape,
                    color = DeepGraphite,
                    border = BorderStroke(2.dp, Color.White),
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit photo",
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${firstName} ${lastName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = DeepGraphite,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Form Fields
            RambleTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = "First name",
                required = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            RambleTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = "Last name",
                required = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            RambleTextField(
                value = email,
                onValueChange = { email = it },
                label = "E-mail",
                required = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            RambleTextField(
                value = birthDate,
                onValueChange = { },
                label = "Date of birth",
                required = false,
                readOnly = true,
                onClick = { showDatePicker = true },
                trailingIcon = {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                }
            )

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val date = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                val formattedDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                birthDate = formattedDate
                            }
                            showDatePicker = false
                        }) {
                            Text("OK", color = DeepGraphite)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel", color = DeepGraphite)
                        }
                    }
                ) {
                    DatePicker(
                        state = datePickerState,
                        colors = DatePickerDefaults.colors(
                            selectedDayContainerColor = DeepGraphite,
                            todayContentColor = DeepGraphite,
                            todayDateBorderColor = DeepGraphite
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(modifier = Modifier.weight(0.35f)) {
                    RambleTextField(
                        value = phoneCountryCode,
                        onValueChange = { phoneCountryCode = it },
                        label = "",
                        leadingIcon = {
                            Text("🇫🇷", modifier = Modifier.padding(start = 12.dp))
                        },
                        trailingIcon = {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    )
                }
                Box(modifier = Modifier.weight(0.65f)) {
                    RambleTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = "Phone",
                        required = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            saveError?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedButton(
                onClick = { /* TODO */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, UIBorderGray),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepGraphite)
            ) {
                Text("Change password", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        // Bottom Button
        Box(modifier = Modifier.padding(24.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        saveError = null
                        runCatching {
                            AuthRepository.updateUserInfo(
                                firstName = firstName,
                                lastName = lastName,
                                email = email,
                                birthDate = birthDate,
                                phoneCountryCode = phoneCountryCode,
                                phoneNumber = phoneNumber
                            )
                            selectedAvatarUri?.let { uri ->
                                val image = readProfileImage(context, uri)
                                avatarUrl = AuthRepository.updateProfilePicture(
                                    imageBytes = image.bytes,
                                    contentType = image.contentType
                                )
                            }
                        }.onSuccess {
                            ProfileRepository.updateAccountInfo(
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                email = email.trim(),
                                birthDate = birthDate.trim(),
                                phoneCountryCode = phoneCountryCode.trim(),
                                phoneNumber = phoneNumber.trim()
                            )
                            ProfileRepository.updateAvatarUrl(avatarUrl)
                            onDismiss()
                        }.onFailure { error ->
                            saveError = error.message ?: "Unable to save profile changes."
                        }
                        isSaving = false
                    }
                },
                enabled = !isSaving && firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepGraphite,
                    disabledContainerColor = UIBackgroundGray
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Save changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class ProfileImageSelection(
    val bytes: ByteArray,
    val contentType: String
)

private suspend fun readProfileImage(context: Context, uri: Uri): ProfileImageSelection =
    withContext(Dispatchers.IO) {
        val contentType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes()
        } ?: error("Unable to read selected profile picture.")

        ProfileImageSelection(
            bytes = bytes,
            contentType = contentType
        )
    }

@Composable
fun RambleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    required: Boolean = false,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) {
            Row {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                if (required) {
                    Text(
                        text = " *",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                readOnly = readOnly,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepGraphite,
                    unfocusedBorderColor = UIBorderGray,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                singleLine = true
            )

            if (onClick != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(onClick = onClick)
                )
            }
        }
    }
}

@Composable
fun Chip(
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        color = if (selected) DeepGraphite else Color.White,
        shape = RoundedCornerShape(50),
        border = if (selected) null else BorderStroke(1.dp, UIBorderGray),
        modifier = Modifier.clickable { onClick() }
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

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    RambleTheme {
        ProfileScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun EditProfilePreview() {
    RambleTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f),
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                EditProfileContent(
                    profile = UserProfile(),
                    onDismiss = {}
                )
            }
        }
    }
}
