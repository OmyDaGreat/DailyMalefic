package xyz.malefic.daily

import org.http4k.core.Body
import org.http4k.format.Jackson.auto
import java.time.LocalDate
import java.time.format.DateTimeParseException

val entryLens = Body.auto<Entry>().toLens()
val entryListLens = Body.auto<List<Entry>>().toLens()

fun parseEntryId(rawId: String?): Long? = rawId?.toLongOrNull()

fun isValidLatestQuery(rawLatest: String?): Boolean =
    rawLatest == null || rawLatest.equals("true", ignoreCase = true) || rawLatest.equals("false", ignoreCase = true)

fun isLatestOnly(rawLatest: String?): Boolean = rawLatest.equals("true", ignoreCase = true)

fun parseDateOrNull(rawDate: String): LocalDate? =
    try {
        LocalDate.parse(rawDate)
    } catch (_: DateTimeParseException) {
        null
    }
