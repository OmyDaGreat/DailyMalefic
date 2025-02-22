package xyz.malefic.daily

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.DebuggingFilters.PrintRequest
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Undertow
import org.http4k.server.asServer
import xyz.malefic.daily.formats.Quote
import xyz.malefic.daily.formats.quoteLens

var currentQuote = Quote("Unknown", "No quote available")

val app: HttpHandler =
    routes(
        "/ping" bind GET to {
            Response(OK).body("pong")
        },
        "/quote" bind GET to {
            Response(OK).with(quoteLens of currentQuote)
        },
        "/quote" bind POST to { request ->
            val newQuote = quoteLens(request)
            currentQuote = newQuote
            Response(OK).with(quoteLens of currentQuote)
        },
    )

fun main() {
    val printingApp: HttpHandler = PrintRequest().then(app)

    val server = printingApp.asServer(Undertow(7290)).start()

    println("Server started on " + server.port())
}
