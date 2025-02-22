package xyz.malefic.daily

import io.kotest.matchers.shouldBe
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.kotest.shouldHaveStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import xyz.malefic.daily.formats.Quote
import xyz.malefic.daily.formats.quoteLens

class DailyMaleficTest {
    @Test
    fun `Ping test`() {
        assertEquals(Response(OK).body("pong"), app(Request(GET, "/ping")))
    }

    @Test
    fun `Post and get quote`() {
        val newQuote = Quote("Author", "This is a new quote")
        val postRequest = Request(POST, "/quote").with(quoteLens of newQuote)
        val postResponse = app(postRequest)

        postResponse shouldHaveStatus OK
        quoteLens(postResponse) shouldBe newQuote

        val getResponse = app(Request(GET, "/quote"))

        getResponse shouldHaveStatus OK
        quoteLens(getResponse) shouldBe newQuote
    }
}
