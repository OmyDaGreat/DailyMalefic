package xyz.malefic.daily

import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.AllowAllOriginPolicy
import org.http4k.filter.CorsPolicy
import org.http4k.filter.DebuggingFilters.PrintRequest
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Undertow
import org.http4k.server.asServer

fun createApp(
    storage: EntryStorage,
    apiKey: String? = System.getenv("API_KEY"),
): HttpHandler {
    val corsPolicy =
        CorsPolicy(
            headers = listOf("Content-Type", API_KEY_HEADER),
            methods = listOf(GET, POST, PUT, DELETE),
            originPolicy = AllowAllOriginPolicy,
        )

    val corsFilter = ServerFilters.Cors(corsPolicy)

    return corsFilter.then(
        routes(
            "/api/ping" bind GET to { Response(OK).body("pong") },
            "/api/health" bind GET to { Response(OK).body("healthy") },
            "/api/auth/validate" bind GET to authValidateHandler(apiKey),
            "/api/entries" bind GET to listEntriesHandler(storage),
            "/api/entries/{id}" bind GET to getEntryByIdHandler(storage),
            "/api/entries" bind POST to createEntryHandler(storage, apiKey),
            "/api/entries/{id}" bind PUT to updateEntryHandler(storage, apiKey),
            "/api/entries/{id}" bind DELETE to deleteEntryHandler(storage, apiKey),
        ),
    )
}

val app: HttpHandler by lazy { createApp(EntryStorage()) }

fun main() {
    val printingApp: HttpHandler = PrintRequest().then(app)
    val server = printingApp.asServer(Undertow(7290)).start()

    println("Server started on port ${server.port()}!")
}
