package xyz.malefic.daily

import dev.toastbits.ytmkt.endpoint.SearchType
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSong

/**
 * Music-related helpers for YouTube Music integration.
 *
 * This object exposes a small API for searching songs by free-text query and
 * returning a best-effort first match as a [YtmSong].
 */
object Music {
    /**
     * Shared YouTube Music API client configured with the `en-US` locale.
     */
    val ytm = YoutubeiApi("en-US")

    /**
     * Searches YouTube Music for the given query and returns the first song match.
     *
     * Search behavior:
     * - Uses the `SONG` search type parameters.
     * - Picks the first [YtmSong] in the returned layouts.
     * - Attempts to load complete song details via `LoadSong`.
     * - Falls back to the search result song if full loading fails.
     *
     * Failure behavior:
     * - Returns `null` if no results are found.
     *
     * @param query Free-text song query (e.g., title, artist, or both).
     * @return The first matched [YtmSong], or `null` if unavailable.
     */
    suspend fun search(query: String): YtmSong? {
        val firstSong: YtmSong =
            ytm.Search
                .search(
                    query,
                    SearchType.SONG.getDefaultParams(),
                    false,
                ).getOrNull()
                ?.categories
                ?.asSequence()
                ?.flatMap { it.first.items.asSequence() }
                ?.filterIsInstance<YtmSong>()
                ?.firstOrNull()
                ?: return null

        return ytm.LoadSong.loadSong(firstSong.id).getOrNull() ?: firstSong
    }
}

/**
 * Converts a YouTube Music song to an entry song model.
 *
 * This extension function transforms a [YtmSong] from the YouTube Music API
 * into an [EntrySong] used by the application's data model. It preserves the
 * song's ID and name, and converts the artist list to [EntrySongArtist] objects.
 *
 * @return An [EntrySong] containing the song's ID, name, and converted artist list.
 *         If the source song has no artists, an empty list is used.
 */
fun YtmSong.toEntrySong(): EntrySong =
    EntrySong(
        id = id,
        name = name,
        artists =
            artists
                .orEmpty()
                .map { artist ->
                    EntrySongArtist(
                        id = artist.id,
                        name = artist.name,
                    )
                },
    )
