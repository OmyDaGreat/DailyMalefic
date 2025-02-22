package xyz.malefic.daily

import io.kotest.matchers.shouldBe
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.kotest.shouldHaveStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import xyz.malefic.daily.format.Quote
import xyz.malefic.daily.format.quoteLens
import xyz.malefic.daily.storage.QuoteStorage
import java.nio.file.Path

class DailyMaleficTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var storage: QuoteStorage
    private lateinit var testApp: HttpHandler

    @BeforeEach
    fun setup() {
        storage = QuoteStorage(tempDir.toString())
        testApp = createApp(storage)
    }

    @Test
    fun `Ping test`() {
        assertEquals(Response(OK).body("pong"), testApp(Request(GET, "/ping")))
    }

    @Test
    fun `Post and get quote`() {
        val newQuote = Quote("Author", "This is a new quote")
        val postRequest = Request(POST, "/quote").with(quoteLens of newQuote)
        val postResponse = testApp(postRequest)

        postResponse shouldHaveStatus OK
        quoteLens(postResponse) shouldBe newQuote

        val getResponse = testApp(Request(GET, "/quote"))

        getResponse shouldHaveStatus OK
        quoteLens(getResponse) shouldBe newQuote
    }

    @Test
    fun `Quote persists after server restart`() {
        // Post a quote
        val newQuote = Quote("Persistence Author", "This quote should persist")
        val postRequest = Request(POST, "/quote").with(quoteLens of newQuote)
        testApp(postRequest)

        // Create new app instance (simulating restart)
        val newStorage = QuoteStorage(tempDir.toString())
        val newApp = createApp(newStorage)

        // Get quote from new instance
        val getResponse = newApp(Request(GET, "/quote"))

        getResponse shouldHaveStatus OK
        quoteLens(getResponse) shouldBe newQuote
    }

    @Test
    fun `Initial quote is default when no stored quote exists`() {
        val getResponse = testApp(Request(GET, "/quote"))

        getResponse shouldHaveStatus OK
        val quote = quoteLens(getResponse)
        quote.author shouldBe "Unknown"
        quote.text shouldBe "No quote available"
    }
}
