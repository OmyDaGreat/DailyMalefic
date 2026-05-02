package xyz.malefic.daily

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.AllowAllOriginPolicy
import org.http4k.filter.CorsPolicy
import org.http4k.filter.DebuggingFilters.PrintRequest
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Undertow
import org.http4k.server.asServer
import xyz.malefic.daily.format.entryHistoryLens
import xyz.malefic.daily.format.entryLens
import xyz.malefic.daily.storage.EntryStorage

fun createApp(
    storage: EntryStorage,
    apiKey: String? = System.getenv("API_KEY"),
): HttpHandler {
    val corsPolicy =
        CorsPolicy(
            headers = listOf("Content-Type"),
            methods = listOf(GET, POST),
            originPolicy = AllowAllOriginPolicy,
        )

    val getRoutes =
        routes(
            "/ping" bind GET to {
                Response(OK).body("pong")
            },
            "/health" bind GET to {
                Response(OK).body("healthy")
            },
            "/entry" bind GET to {
                Response(OK).with(entryLens of storage.loadEntry())
            },
            "/entry/history" bind GET to {
                Response(OK).with(entryHistoryLens of storage.loadHistory())
            },
        )

    val postRoutes =
        routes(
            "/entry" bind POST to { request ->
                // API key authentication
                val requestApiKey = request.header("X-API-Key")
                if (apiKey != null && requestApiKey != apiKey) {
                    Response(UNAUTHORIZED).body("Invalid or missing API key")
                } else {
                    val newEntry = entryLens(request)
                    storage.saveEntry(newEntry)
                    Response(OK).with(entryLens of newEntry)
                }
            },
        )

    val corsFilter = ServerFilters.Cors(corsPolicy)

    return corsFilter.then(
        routes(
            "" bind GET to getRoutes,
            "" bind POST to postRoutes,
        ),
    )
}

val app: HttpHandler by lazy { createApp(EntryStorage()) }

fun main() {
    val printingApp: HttpHandler = PrintRequest().then(app)

    val server = printingApp.asServer(Undertow(7290)).start()

    println("Server started on port ${server.port()}!")
}
