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
import org.http4k.filter.AnyOf
import org.http4k.filter.CorsPolicy
import org.http4k.filter.DebuggingFilters.PrintRequest
import org.http4k.filter.OriginPolicy
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Undertow
import org.http4k.server.asServer
import xyz.malefic.daily.format.quoteHistoryLens
import xyz.malefic.daily.format.quoteLens
import xyz.malefic.daily.storage.QuoteStorage

fun createApp(
    storage: QuoteStorage,
    test: Boolean = false,
    apiKey: String? = System.getenv("API_KEY"),
): HttpHandler {
    val postOriginList = listOf("https://monophobia.malefic.xyz")

    val getRequestsCorsPolicy =
        CorsPolicy(
            headers = listOf("Content-Type"),
            methods = listOf(GET),
            originPolicy = AllowAllOriginPolicy,
        )

    val postRequestsCorsPolicy =
        CorsPolicy(
            headers = listOf("Content-Type", "X-API-Key"),
            methods = listOf(POST),
            originPolicy =
                OriginPolicy
                    .AnyOf(postOriginList)
                    .takeUnless { test } ?: AllowAllOriginPolicy,
        )

    val getRoutes =
        routes(
            "/ping" bind GET to {
                Response(OK).body("pong")
            },
            "/health" bind GET to {
                Response(OK).body("healthy")
            },
            "/quote" bind GET to {
                Response(OK).with(quoteLens of storage.loadQuote())
            },
            "/quote/history" bind GET to {
                Response(OK).with(quoteHistoryLens of storage.loadHistory())
            },
        )

    val postRoutes =
        routes(
            "/quote" bind POST to { request ->
                // API key authentication
                val requestApiKey = request.header("X-API-Key")
                if (apiKey != null && requestApiKey != apiKey) {
                    Response(UNAUTHORIZED).body("Invalid or missing API key")
                } else {
                    val newQuote = quoteLens(request)
                    storage.saveQuote(newQuote)
                    Response(OK).with(quoteLens of newQuote)
                }
            },
        )

    val corsGetRoutes = ServerFilters.Cors(getRequestsCorsPolicy).then(getRoutes)
    val corsPostRoutes = ServerFilters.Cors(postRequestsCorsPolicy).then(postRoutes)

    return routes(
        "" bind GET to corsGetRoutes,
        "" bind POST to corsPostRoutes,
    )
}

val app: HttpHandler by lazy { createApp(QuoteStorage()) }

fun main() {
    val printingApp: HttpHandler = PrintRequest().then(app)

    val server = printingApp.asServer(Undertow(7290)).start()

    println("Server started on " + server.port())
}
