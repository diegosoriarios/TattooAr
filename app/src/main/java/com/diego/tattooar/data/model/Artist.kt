package com.diego.tattooar.data.model

data class Artist(
    val id: String,
    val name: String,
    val studioName: String,
    val city: String,
    val rating: Float,
    val specialties: List<String>,
    val instagramHandle: String,
    val bio: String
) {
    companion object {
        val SAMPLE_ARTISTS = listOf(
            Artist(
                id = "artist_1",
                name = "Elena Vance",
                studioName = "Black Ink Atelier",
                city = "New York, NY",
                rating = 4.9f,
                specialties = listOf("Black & Grey", "Realism", "Fine Line"),
                instagramHandle = "@elenavance_ink",
                bio = "Specializing in detailed fine-line realism and custom black & grey tattoo design."
            ),
            Artist(
                id = "artist_2",
                name = "Marcus Thorne",
                studioName = "Nordic Tattoo Lab",
                city = "Seattle, WA",
                rating = 4.8f,
                specialties = listOf("Tribal", "Geometric", "Dotwork"),
                instagramHandle = "@marcusthorne_tattoo",
                bio = "Creating modern geometric patterns, sacred dotwork, and large-scale tribal sleeves."
            ),
            Artist(
                id = "artist_3",
                name = "Sora Lin",
                studioName = "Lotus Tattoo Co.",
                city = "San Francisco, CA",
                rating = 4.95f,
                specialties = listOf("Floral", "Watercolor", "Neo-Traditional"),
                instagramHandle = "@sora_lin_art",
                bio = "Illustrative neo-traditional floral sleeves and vibrant custom watercolor pieces."
            )
        )
    }
}
