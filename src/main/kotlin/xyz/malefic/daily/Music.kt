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

suspend fun main() {
    println(Music.search("Bohemian Rhapsody"))
}
