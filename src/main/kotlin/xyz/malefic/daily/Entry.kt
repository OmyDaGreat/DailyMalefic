package xyz.malefic.daily

import com.fasterxml.jackson.annotation.JsonInclude
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSong
import org.http4k.core.Body
import org.http4k.format.Jackson.auto
import java.time.LocalDate

/**
 * Data class representing an entry with an author, text, and date.
 *
 * @property author The author of the entry.
 * @property text The text of the entry.
 * @property date The date of the entry, defaulting to the current date.
 * @property song The song associated with the entry (optional).
 * @property songQuery A transient query string to search for a song (request-only, not persisted).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Entry(
    val author: String,
    val text: String,
    val date: LocalDate = LocalDate.now(),
    val song: YtmSong? = null,
    val songQuery: String? = null,
)

/**
 * Lens to handle the conversion between HTTP bodies and Entry objects.
 */
val entryLens = Body.auto<Entry>().toLens()

/**
 * Lens to handle the conversion between HTTP bodies and lists of Entry objects.
 */
val entryHistoryLens = Body.auto<List<Entry>>().toLens()
