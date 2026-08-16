package com.ramble.core.ai

object LoadingMessages {
    val tripPlanning = listOf(
        "✨ Trip planning",
        "Planning your perfect getaway…",
        "Building your trip around your budget…",
        "Turning your ideas into an itinerary…",
        "Putting your trip together…",
        "Designing your perfect escape…",
        "Mapping out your adventure…",
        "Creating your personalized itinerary…",
        "Finding the best way to spend your time…",
        "Making your trip fit just right…",
        "Putting the pieces together…"
    )

    val discoveringDestination = listOf(
        "🗺️ Discovering the destination",
        "Exploring the best of your destination…",
        "Finding places worth discovering…",
        "Looking beyond the usual tourist spots…",
        "Searching for hidden gems…",
        "Finding the places locals love…",
        "Exploring neighborhoods you might enjoy…",
        "Picking experiences you'll remember…",
        "Finding your kind of places…",
        "Discovering what makes this destination special…"
    )

    val budget = listOf(
        "💰 Budget",
        "Making every euro count…",
        "Balancing your trip with your budget…",
        "Finding great experiences without overspending…",
        "Optimizing your budget…",
        "Checking prices and costs…",
        "Finding the best value for your money…",
        "Making room for the experiences that matter…",
        "Keeping your budget on track…"
    )

    val foodAndExperiences = listOf(
        "🍜 Food & experiences",
        "Finding places you'll love to eat…",
        "Hunting down the best local food…",
        "Adding a few delicious stops…",
        "Finding experiences worth making time for…",
        "Mixing must-sees with hidden gems…",
        "Finding the perfect balance of food, culture and fun…",
        "Picking experiences for the whole group…",
        "Finding your kind of places…"
    )

    val groupTrips = listOf(
        "👥 Group trips",
        "Making sure everyone gets a say…",
        "Balancing everyone's interests…",
        "Finding something for everyone…",
        "Building a trip your whole group will enjoy…",
        "Making the itinerary work for everyone…",
        "Finding the right mix of activities…",
        "Keeping the group budget in balance…"
    )

    val logistics = listOf(
        "🚆 Logistics",
        "Connecting the dots…",
        "Optimizing your route…",
        "Making your days flow smoothly…",
        "Checking travel times between stops…",
        "Avoiding unnecessary detours…",
        "Finding the smartest way around…",
        "Making sure you have enough time to enjoy each place…",
        "Putting everything in the right order…"
    )

    val finalTouches = listOf(
        "🌤️ Final touches",
        "Checking the little details…",
        "Giving your itinerary a final polish…",
        "Making a few last adjustments…",
        "Double-checking your schedule…",
        "Adding a few finishing touches…",
        "Making sure everything fits…",
        "Your trip is almost ready…",
        "Putting it all together…",
        "One last look…",
        "Your adventure is taking shape…"
    )

    val playful = listOf(
        "❤️ More playful Ramble-style messages",
        "Your suitcase is practically packed…",
        "We found a few places you’re going to love…",
        "Somewhere between planning and daydreaming…",
        "Your next adventure is coming together…",
        "Good trips take a little planning…",
        "We’re doing the boring part for you…",
        "Coffee stops are being strategically placed…",
        "Finding the perfect spot for sunset…",
        "Making sure you don't spend €40 on a sandwich…",
        "Checking if that “hidden gem” is actually worth it…",
        "Your future self is going to love this itinerary…",
        "We’re making room for spontaneous moments…",
        "Because getting lost is fun… when it’s intentional.",
        "Almost there. Your adventure is taking shape."
    )

    fun getRandom(list: List<String>): String = list.drop(1).random()
}
