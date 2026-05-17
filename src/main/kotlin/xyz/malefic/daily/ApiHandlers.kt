package xyz.malefic.daily

import kotlinx.coroutines.runBlocking
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.with
import org.http4k.lens.LensFailure
import org.http4k.routing.path

/**
 * Validates an API key from the request header against the configured API key.
 *
 * @param apiKey The configured API key to validate against, or null if validation is disabled
 * @return An HTTP handler that responds with:
 *   - 200 OK if no API key is configured, or if the request key matches the configured key
 *   - 401 Unauthorized if the request key does not match the configured key
 */
fun authValidateHandler(apiKey: String?): HttpHandler =
    { request ->
        val requestApiKey = request.header(API_KEY_HEADER)
        when {
            apiKey == null -> Response(OK).body("No API key exists")
            requestApiKey == apiKey -> Response(OK).body("Authorization check passed")
            else -> Response(UNAUTHORIZED).body("Invalid API key")
        }
    }

/**
 * Lists entries from storage based on query parameters.
 *
 * Supports filtering by date, author, or retrieving the latest entries. Query parameters:
 * - `date`: Filter by specific date (format: YYYY-MM-DD)
 * - `author`: Filter by author name
 * - `latest`: If true, retrieve only the latest date's entries
 *
 * @param storage The entry storage instance to query
 * @return An HTTP handler that responds with:
 *   - 200 OK with filtered entry list if query is valid
 *   - 400 Bad Request if the date format is invalid or latest parameter is not a boolean
 */
fun listEntriesHandler(storage: EntryStorage): HttpHandler =
    { request ->
        val date = request.query("date")
        val author = request.query("author")
        val latest = request.query("latest")

        when {
            !isValidLatestQuery(latest) -> {
                Response(BAD_REQUEST).body("Invalid latest value, expected true or false")
            }

            !date.isNullOrBlank() -> {
                val parsedDate = parseDateOrNull(date)
                if (parsedDate == null) {
                    Response(BAD_REQUEST).body("Invalid date format, expected YYYY-MM-DD")
                } else {
                    Response(OK).with(entryListLens of storage.loadEntriesByDate(parsedDate))
                }
            }

            !author.isNullOrBlank() -> {
                Response(OK).with(entryListLens of storage.loadEntriesByAuthor(author))
            }

            isLatestOnly(latest) -> {
                Response(OK).with(entryListLens of storage.loadLatestDateEntries())
            }

            else -> {
                Response(OK).with(entryListLens of storage.loadHistory())
            }
        }
    }

/**
 * Retrieves a single entry by its ID.
 *
 * @param storage The entry storage instance to query
 * @return An HTTP handler that responds with:
 *   - 200 OK with the entry if found
 *   - 400 Bad Request if the ID format is invalid (not a number)
 *   - 404 Not Found if no entry exists with the given ID
 */
fun getEntryByIdHandler(storage: EntryStorage): HttpHandler =
    { request ->
        val rawId = request.path("id")
        val id = parseEntryId(rawId)
        if (id == null) {
            Response(BAD_REQUEST).body("Invalid ID format, expected a number")
        } else {
            val found = storage.loadEntryById(id)
            if (found != null) {
                Response(OK).with(entryLens of found)
            } else {
                Response(NOT_FOUND).body("No entry found with id $rawId")
            }
        }
    }

/**
 * Creates a new entry in storage.
 *
 * The request body should contain an Entry object without an ID (ID must be 0).
 * Validates API key authorization and normalizes song information via music search.
 *
 * @param storage The entry storage instance to save to
 * @param apiKey The configured API key for authorization, or null to skip validation
 * @return An HTTP handler that responds with:
 *   - 201 Created with the saved entry and Location header if successful
 *   - 400 Bad Request if the entry ID is provided, or entry format is invalid
 *   - 401 Unauthorized if the API key is missing or invalid
 * @throws LensFailure if the request body cannot be parsed as an Entry
 */
fun createEntryHandler(
    storage: EntryStorage,
    apiKey: String?,
): HttpHandler =
    { request ->
        if (isUnauthorized(apiKey, request.header(API_KEY_HEADER))) {
            Response(UNAUTHORIZED).body("Invalid or missing API key")
        } else {
            try {
                val requestEntry = entryLens(request)
                if (requestEntry.id != 0L) {
                    Response(BAD_REQUEST).body("ID must not be provided when creating an entry")
                } else {
                    val savedEntry = storage.saveEntry(normalizeSong(requestEntry.copy(id = 0L)))
                    Response(CREATED)
                        .header("Location", "/api/entries/${savedEntry.id}")
                        .with(entryLens of savedEntry)
                }
            } catch (e: LensFailure) {
                Response(BAD_REQUEST).body("Invalid entry format: ${e.message}")
            }
        }
    }

/**
 * Updates an existing entry in storage.
 *
 * The request body should contain an Entry object with the same ID as the path parameter.
 * Validates API key authorization and normalizes song information via music search.
 *
 * @param storage The entry storage instance to update
 * @param apiKey The configured API key for authorization, or null to skip validation
 * @return An HTTP handler that responds with:
 *   - 200 OK with the updated entry if successful
 *   - 400 Bad Request if the ID format is invalid or the entry ID in body doesn't match path ID
 *   - 401 Unauthorized if the API key is missing or invalid
 *   - 404 Not Found if no entry exists with the given ID
 * @throws LensFailure if the request body cannot be parsed as an Entry
 */
fun updateEntryHandler(
    storage: EntryStorage,
    apiKey: String?,
): HttpHandler =
    { request ->
        if (isUnauthorized(apiKey, request.header(API_KEY_HEADER))) {
            Response(UNAUTHORIZED).body("Invalid or missing API key")
        } else {
            val rawId = request.path("id")
            val id = parseEntryId(rawId)
            when {
                id == null -> {
                    Response(BAD_REQUEST).body("Invalid ID format, expected a number")
                }

                storage.loadEntryById(id) == null -> {
                    Response(NOT_FOUND).body("No entry found with id $rawId")
                }

                else -> {
                    try {
                        val requestEntry = entryLens(request)
                        if (requestEntry.id != 0L && requestEntry.id != id) {
                            Response(BAD_REQUEST).body("Entry ID in body must match path ID")
                        } else {
                            val savedEntry = storage.saveEntry(normalizeSong(requestEntry.copy(id = id)))
                            Response(OK).with(entryLens of savedEntry)
                        }
                    } catch (e: LensFailure) {
                        Response(BAD_REQUEST).body("Invalid entry format: ${e.message}")
                    }
                }
            }
        }
    }

/**
 * Deletes an entry from storage by its ID.
 *
 * Validates API key authorization before allowing deletion.
 *
 * @param storage The entry storage instance to delete from
 * @param apiKey The configured API key for authorization, or null to skip validation
 * @return An HTTP handler that responds with:
 *   - 204 No Content if the entry was successfully deleted
 *   - 400 Bad Request if the ID format is invalid (not a number)
 *   - 401 Unauthorized if the API key is missing or invalid
 *   - 404 Not Found if no entry exists with the given ID
 */
fun deleteEntryHandler(
    storage: EntryStorage,
    apiKey: String?,
): HttpHandler =
    { request ->
        if (isUnauthorized(apiKey, request.header(API_KEY_HEADER))) {
            Response(UNAUTHORIZED).body("Invalid or missing API key")
        } else {
            val rawId = request.path("id")
            val id = parseEntryId(rawId)
            when {
                id == null -> Response(BAD_REQUEST).body("Invalid ID format, expected a number")
                storage.deleteEntry(id) -> Response(NO_CONTENT)
                else -> Response(NOT_FOUND).body("No entry found with id $rawId")
            }
        }
    }

/**
 * Determines if the request is unauthorized based on API key configuration and request header.
 *
 * @param apiKey The configured API key, or null if authorization is disabled
 * @param requestApiKey The API key from the request header, or null if not provided
 * @return true if authorization is configured but the request key doesn't match, false otherwise
 */
private fun isUnauthorized(
    apiKey: String?,
    requestApiKey: String?,
): Boolean = apiKey != null && requestApiKey != apiKey

/**
 * Normalizes an entry by searching for and resolving song information.
 *
 * If the entry contains a songQuery, searches for the song using the Music service
 * and updates the entry with the song result. Clears the songQuery field regardless.
 *
 * @param entry The entry to normalize
 * @return A new entry with normalized song information and cleared songQuery
 */
private fun normalizeSong(entry: Entry): Entry =
    if (!entry.songQuery.isNullOrBlank()) {
        val foundSong =
            runBlocking {
                Music.search(entry.songQuery)
            }
        entry.copy(song = foundSong?.toEntrySong(), songQuery = null)
    } else {
        entry.copy(songQuery = null)
    }
