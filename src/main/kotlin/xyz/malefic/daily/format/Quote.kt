package xyz.malefic.daily.format

import org.http4k.core.Body
import org.http4k.format.Jackson.auto

/**
 * Data class representing a Quote with an author and text.
 *
 * @property author The author of the quote.
 * @property text The text of the quote.
 */
data class Quote(
    val author: String,
    val text: String,
)

/**
 * Lens to handle the conversion between HTTP bodies and Quote objects.
 */
val quoteLens = Body.auto<Quote>().toLens()
