# Architectural Sketch: Deterministic Day Skeleton Generation Stage

## Motivation
Currently, the LLM is responsible for both structuring the day (deciding how many food, activity, and nightlife slots exist and when they occur) and selecting specific POIs to fill those slots. This double responsibility occasionally leads to category mismatches, inconsistent step counts, or unbalanced daily schedules.

## Proposed Architecture
Introduce a deterministic `DaySkeletonGenerator` phase between `CandidateFilterEngine` and `AiPlannerService.generateTripChunk()`:

```
[Raw OSM POIs & Weather]
          │
          ▼
[CandidateFilterEngine] (Ranks & filters POIs)
          │
          ▼
[DaySkeletonGenerator]  (Builds deterministic slot skeletons per day)
          │
          ▼
[AiPlannerService]     (Asks LLM to narrate & bind POIs to pre-assigned slots)
          │
          ▼
[TripItineraryValidator] (Validates final JSON shape & constraints)
```

## Data Schema Proposal

```kotlin
enum class SlotCategory {
    BREAKFAST,
    LUNCH,
    DINNER,
    CAFE,
    MAIN_ATTRACTION,
    SECONDARY_ATTRACTION,
    NIGHTLIFE_EVENING,
    ACCOMMODATION_CHECKIN,
    ACCOMMODATION_OVERNIGHT,
    TRANSPORT,
    FREE_TIME
}

data class DailySlotSkeleton(
    val timeSlot: String,             // e.g. "09:00 AM", "01:00 PM", "08:00 PM"
    val slotCategory: SlotCategory,
    val allowedOsmCategories: List<String>, // e.g. ["museum", "gallery", "historic"]
    val candidatePoiPool: List<POI>   // Pre-filtered matching POIs for this exact slot
)

data class DaySkeleton(
    val dayNumber: Int,
    val date: LocalDate,
    val suggestedTheme: String,
    val slots: List<DailySlotSkeleton>
)
```

## Deterministic Rule Engine
The skeleton generator creates `DaySkeleton`s using rule templates based on traveler profile and trip constraints:

1. **Standard Full Day (Balanced Travel Style)**:
   - 08:30 AM — Breakfast (`amenity=cafe|bakery`)
   - 10:00 AM — Main Attraction (`tourism=museum|attraction|gallery`)
   - 01:00 PM — Lunch (`amenity=restaurant|bistro`)
   - 03:00 PM — Secondary Attraction (`leisure=park|nature_reserve` or `shop=mall|boutique`)
   - 07:30 PM — Dinner (`amenity=restaurant`)
   - 10:00 PM — Nightlife (only if `Interests` contains "Nightlife": `amenity=bar|pub|nightclub`)

2. **Arrival / Departure Pacing Adjustment**:
   - Day 1: Adds `ACCOMMODATION_CHECKIN` and restricts morning activity.
   - Final Day: Adds departure `TRANSPORT` step and truncates evening slots.

## Advantages & Impact
- **Guaranteed Category Separation**: Gemini receives explicit instructions per slot (e.g., "Slot 2 (10:00 AM) MUST be selected from the MAIN_ATTRACTION candidate list").
- **Eliminates Validation Retries**: Prevents pure lodging or miscategorized POIs from being scheduled in activity slots before LLM invocation.
- **Foundation for Dynamic Re-planning**: Enables single-slot replacement (e.g. replacing an outdoor park slot with an indoor museum slot when rain is detected) without re-generating the full multi-day itinerary.
