package xyz.malefic.daily

import io.kotest.matchers.shouldBe
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.kotest.shouldHaveStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import xyz.malefic.daily.format.Quote
import xyz.malefic.daily.format.quoteHistoryLens
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
        testApp = createApp(storage, test = true, apiKey = null)
    }

    @Test
    fun `Ping test`() {
        val response = testApp(Request(GET, "/ping"))
        response shouldHaveStatus OK
        response.bodyString() shouldBe "pong"
    }

    @Test
    fun `Health check test`() {
        val response = testApp(Request(GET, "/health"))
        response shouldHaveStatus OK
        response.bodyString() shouldBe "healthy"
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
        val newApp = createApp(newStorage, test = true, apiKey = null)

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

    @Test
    fun `Quote history tracks all saved quotes`() {
        val quote1 = Quote("Author 1", "First quote")
        val quote2 = Quote("Author 2", "Second quote")

        testApp(Request(POST, "/quote").with(quoteLens of quote1))
        testApp(Request(POST, "/quote").with(quoteLens of quote2))

        val historyResponse = testApp(Request(GET, "/quote/history"))
        historyResponse shouldHaveStatus OK

        val history = quoteHistoryLens(historyResponse)
        history.size shouldBe 3 // default + 2 new quotes
        history[1].author shouldBe "Author 1"
        history[1].text shouldBe "First quote"
        history[2].author shouldBe "Author 2"
        history[2].text shouldBe "Second quote"
    }
}
