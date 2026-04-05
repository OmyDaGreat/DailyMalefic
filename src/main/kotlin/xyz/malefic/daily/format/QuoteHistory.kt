package xyz.malefic.daily.format

import org.http4k.core.Body
import org.http4k.format.Jackson.auto
import java.time.Instant

/**
 * Data class representing a Quote with history metadata.
 *
 * @property author The author of the quote.
 * @property text The text of the quote.
 * @property timestamp The timestamp when the quote was saved.
 */
data class QuoteWithTimestamp(
    val author: String,
    val text: String,
    val timestamp: Instant,
)

/**
 * Lens to handle the conversion between HTTP bodies and QuoteWithTimestamp list.
 */
val quoteHistoryLens = Body.auto<List<QuoteWithTimestamp>>().toLens()
