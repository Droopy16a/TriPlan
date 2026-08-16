package com.ramble.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramble.core.ai.SavedTrip
import com.ramble.core.designsystem.theme.DeepGraphite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: HomeViewModel,
    onTripClick: (SavedTrip) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.exploreSearchQuery.collectAsState()
    val searchResults by viewModel.exploreSearchResults.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F7F5))
            .statusBarsPadding()
    ) {
        // Search Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Text(
                text = "Explore",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = DeepGraphite,
                letterSpacing = (-1).sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Find community published trips",
                fontSize = 16.sp,
                color = Color(0xFF888888),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateExploreSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp)),
                placeholder = { Text("Search by city or country...", color = Color(0xFFBBBBBB)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = DeepGraphite
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
        }

        // Group by country (everything after the last comma)
        val groupedTrips = searchResults.groupBy { 
            it.destination.substringAfterLast(",").trim() 
        }

        // Results Grid
        LazyColumn(
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            groupedTrips.forEach { (country, trips) ->
                item {
                    Column {
                        Text(
                            text = country,
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 22.sp,
                            color = DeepGraphite,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                        )
                        
                        androidx.compose.foundation.lazy.LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(trips.size, key = { trips[it].id }) { index ->
                                val trip = trips[index]
                                PopularTripCard(
                                    trip = trip,
                                    onClick = { onTripClick(trip) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
