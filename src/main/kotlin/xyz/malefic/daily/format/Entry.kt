package xyz.malefic.daily.format

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.http4k.core.Body
import org.http4k.format.Jackson.auto
import kotlin.time.Clock

/**
 * Data class representing an entry with an author, text, and date.
 *
 * @property author The author of the entry.
 * @property text The text of the entry.
 * @property date The date of the entry, defaulting to the current date.
 */
data class Entry(
    val author: String,
    val text: String,
    val date: LocalDate =
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date,
)

/**
 * Lens to handle the conversion between HTTP bodies and Entry objects.
 */
val entryLens = Body.auto<Entry>().toLens()

/**
 * Lens to handle the conversion between HTTP bodies and lists of Entry objects.
 */
val entryHistoryLens = Body.auto<List<Entry>>().toLens()
