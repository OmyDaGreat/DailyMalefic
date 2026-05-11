package xyz.malefic.daily

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSong
import org.http4k.core.Body
import org.http4k.format.Jackson.auto
import java.time.LocalDate
import java.util.UUID

/**
 * Data class representing an entry with an author, text, and date.
 *
 * @property id A unique identifier for the entry.
 * @property author The author of the entry.
 * @property text The text of the entry.
 * @property date The date of the entry, defaulting to the current date.
 * @property song A minimal song summary associated with the entry (optional).
 * @property songQuery A transient query string to search for a song (request-only, not persisted).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Entry(
    val id: String = UUID.randomUUID().toString(),
    val author: String,
    val text: String,
    val date: LocalDate = LocalDate.now(),
    val song: EntrySong? = null,
    val songQuery: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EntrySongArtist(
    val id: String,
    val name: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EntrySong(
    val id: String,
    val name: String? = null,
    val artists: List<EntrySongArtist> = emptyList(),
)

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

/**
 * Lens to handle the conversion between HTTP bodies and Entry objects.
 */
val entryLens = Body.auto<Entry>().toLens()

/**
 * Lens to handle the conversion between HTTP bodies and lists of Entry objects.
 */
val entryHistoryLens = Body.auto<List<Entry>>().toLens()
