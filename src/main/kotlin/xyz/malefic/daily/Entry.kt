package xyz.malefic.daily

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import java.time.LocalDate

/**
 * Data class representing an entry with an author, text, and date.
 *
 * @property id A server-assigned identifier for the entry. 0 indicates not yet assigned.
 * @property author The author of the entry.
 * @property text The text of the entry.
 * @property date The date of the entry, defaulting to the current date.
 * @property song A minimal song summary associated with the entry (optional).
 * @property songQuery A transient query string to search for a song (request-only, not persisted).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Entry(
    @param:JsonSetter(nulls = Nulls.SKIP)
    val id: Long = 0L,
    val author: String,
    val text: String,
    val date: LocalDate = LocalDate.now(),
    val song: EntrySong? = null,
    val songQuery: String? = null,
)

/**
 * Data class representing a song with minimal information for entry association.
 *
 * This class provides a lightweight representation of a song, suitable for storing
 * song references within entries. Unknown JSON properties are ignored during deserialization.
 *
 * @property id A unique identifier for the song.
 * @property name The title of the song (optional).
 * @property artists A list of artists associated with the song (defaults to empty list).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class EntrySong(
    val id: String,
    val name: String? = null,
    val artists: List<EntrySongArtist> = emptyList(),
)

/**
 * Data class representing a song artist with minimal information.
 *
 * @property id A unique identifier for the artist.
 * @property name The name of the artist (optional).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class EntrySongArtist(
    val id: String,
    val name: String? = null,
)
