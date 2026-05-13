package xyz.malefic.daily

import kotlinx.coroutines.runBlocking
import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.AllowAllOriginPolicy
import org.http4k.filter.CorsPolicy
import org.http4k.filter.DebuggingFilters.PrintRequest
import org.http4k.filter.ServerFilters
import org.http4k.lens.LensFailure
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Undertow
import org.http4k.server.asServer
import java.time.LocalDate
import java.time.format.DateTimeParseException

private const val API_KEY_HEADER = "X-API-Key"

fun createApp(
    storage: EntryStorage,
    apiKey: String? = System.getenv("API_KEY"),
): HttpHandler {
    val corsPolicy =
        CorsPolicy(
            headers = listOf("Content-Type"),
            methods = listOf(GET, POST, DELETE),
            originPolicy = AllowAllOriginPolicy,
        )

    val getRoutes =
        routes(
            "/ping" bind GET to {
                Response(OK).body("pong")
            },
            "/auth-ping" bind GET to { request ->
                val requestApiKey = request.header(API_KEY_HEADER)
                when (apiKey) {
                    null -> {
                        Response(OK).body("No API key exists")
                    }

                    requestApiKey -> {
                        Response(OK).body("Authorization check passed")
                    }

                    else -> {
                        Response(UNAUTHORIZED).body("Invalid API key")
                    }
                }
            },
            "/health" bind GET to {
                Response(OK).body("healthy")
            },
            "/entry" bind GET to { request ->
                val id = request.query("id")
                val date = request.query("date")
                val author = request.query("author")
                when {
                    !id.isNullOrBlank() -> {
                        val idNum = id.toLongOrNull()
                        if (idNum == null) {
                            Response(BAD_REQUEST).body("Invalid ID format, expected a number")
                        } else {
                            val found = storage.loadEntryById(idNum)
                            if (found != null) {
                                Response(OK).with(entryLens of found)
                            } else {
                                Response(NO_CONTENT).body("No entry found with id $id")
                            }
                        }
                    }

                    !date.isNullOrBlank() -> {
                        try {
                            val found = storage.loadEntriesByDate(LocalDate.parse(date))
                            if (found.isNotEmpty()) {
                                Response(OK).with(entryListLens of found)
                            } else {
                                Response(NO_CONTENT).body("No entries from date $date")
                            }
                        } catch (_: DateTimeParseException) {
                            Response(BAD_REQUEST).body("Invalid date format, expected YYYY-MM-DD")
                        }
                    }

                    !author.isNullOrBlank() -> {
                        val found = storage.loadEntriesByAuthor(author)
                        if (found.isNotEmpty()) {
                            Response(OK).with(entryListLens of found)
                        } else {
                            Response(NO_CONTENT).body("No entries found with author $author")
                        }
                    }

                    else -> {
                        Response(OK).with(entryListLens of storage.loadLatestDateEntries())
                    }
                }
            },
            "/entry/history" bind GET to {
                Response(OK).with(entryListLens of storage.loadHistory())
            },
        )

    val postRoutes =
        routes(
            "/entry" bind POST to { request ->
                val requestApiKey = request.header(API_KEY_HEADER)
                if (apiKey != null && requestApiKey != apiKey) {
                    Response(UNAUTHORIZED).body("Invalid or missing API key")
                } else {
                    try {
                        val requestEntry = entryLens(request)

                        val finalEntry =
                            if (!requestEntry.songQuery.isNullOrBlank()) {
                                val foundSong =
                                    runBlocking {
                                        Music.search(requestEntry.songQuery)
                                    }
                                requestEntry.copy(song = foundSong?.toEntrySong(), songQuery = null)
                            } else {
                                requestEntry.copy(songQuery = null)
                            }

                        val savedEntry = storage.saveEntry(finalEntry)
                        Response(OK).with(entryLens of savedEntry)
                    } catch (e: LensFailure) {
                        Response(BAD_REQUEST).body("Invalid entry format: ${e.message}")
                    }
                }
            },
            "/entry" bind DELETE to { request ->
                val requestApiKey = request.header(API_KEY_HEADER)
                if (apiKey != null && requestApiKey != apiKey) {
                    Response(UNAUTHORIZED).body("Invalid or missing API key")
                } else {
                    val id = request.query("id")
                    if (id.isNullOrEmpty()) {
                        Response(BAD_REQUEST).body("Missing id")
                    } else {
                        val idNum = id.toLongOrNull()
                        if (idNum == null) {
                            Response(BAD_REQUEST).body("Invalid ID format, expected a number")
                        } else if (storage.deleteEntry(idNum)) {
                            Response(OK).body("Entry deleted")
                        } else {
                            Response(NO_CONTENT).body("Entry not found, nothing deleted")
                        }
                    }
                }
            },
        )

    val corsFilter = ServerFilters.Cors(corsPolicy)

    return corsFilter.then(
        routes(
            "" bind GET to getRoutes,
            "" bind POST to postRoutes,
            "" bind DELETE to postRoutes,
        ),
    )
}

val app: HttpHandler by lazy { createApp(EntryStorage()) }

fun main() {
    val printingApp: HttpHandler = PrintRequest().then(app)

    val server = printingApp.asServer(Undertow(7290)).start()

    println("Server started on port ${server.port()}!")
}
