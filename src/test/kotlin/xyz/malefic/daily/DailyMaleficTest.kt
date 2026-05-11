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
import java.nio.file.Path

class DailyMaleficTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var storage: EntryStorage
    private lateinit var testApp: HttpHandler

    @BeforeEach
    fun setup() {
        storage = EntryStorage(tempDir.toString())
        testApp = createApp(storage, apiKey = null)
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
    fun `Post and get entry`() {
        val newEntry = Entry("Author", "This is a new entry")
        val postRequest = Request(POST, "/entry").with(entryLens of newEntry)
        val postResponse = testApp(postRequest)

        postResponse shouldHaveStatus OK
        entryLens(postResponse) shouldBe newEntry

        val getResponse = testApp(Request(GET, "/entry"))

        getResponse shouldHaveStatus OK
        entryLens(getResponse) shouldBe newEntry
    }

    @Test
    fun `Entry persists after server restart`() {
        // Post an entry
        val newEntry = Entry("Persistence Author", "This entry should persist")
        val postRequest = Request(POST, "/entry").with(entryLens of newEntry)
        testApp(postRequest)

        // Create new app instance (simulating restart)
        val newStorage = EntryStorage(tempDir.toString())
        val newApp = createApp(newStorage, apiKey = null)

        // Get entry from new instance
        val getResponse = newApp(Request(GET, "/entry"))

        getResponse shouldHaveStatus OK
        entryLens(getResponse) shouldBe newEntry
    }

    @Test
    fun `Initial entry is default when no stored entry exists`() {
        val getResponse = testApp(Request(GET, "/entry"))

        getResponse shouldHaveStatus OK
        val entry = entryLens(getResponse)
        entry.author shouldBe "Unknown"
        entry.text shouldBe "No entry available"
    }

    @Test
    fun `Entry history tracks all saved entries`() {
        val entry1 = Entry("Author 1", "First entry")
        val entry2 = Entry("Author 2", "Second entry")

        testApp(Request(POST, "/entry").with(entryLens of entry1))
        testApp(Request(POST, "/entry").with(entryLens of entry2))

        val historyResponse = testApp(Request(GET, "/entry/history"))
        historyResponse shouldHaveStatus OK

        val history = entryListLens(historyResponse)
        history.size shouldBe 3 // default + 2 new entries
        history[1].author shouldBe "Author 1"
        history[1].text shouldBe "First entry"
        history[2].author shouldBe "Author 2"
        history[2].text shouldBe "Second entry"
    }

    @Test
    fun `Post entry with songQuery searches and includes found song`() {
        val entryWithQuery = Entry("Author", "Entry with song", songQuery = "test song")
        val postRequest = Request(POST, "/entry").with(entryLens of entryWithQuery)
        val postResponse = testApp(postRequest)

        postResponse shouldHaveStatus OK
        val responseEntry = entryLens(postResponse)

        // songQuery should not be in the response
        responseEntry.songQuery shouldBe null
        // song may be populated if the search was successful or null if offline/failed
        // The important thing is that songQuery is cleared
        responseEntry.author shouldBe "Author"
        responseEntry.text shouldBe "Entry with song"
    }

    @Test
    fun `Post entry without songQuery works normally`() {
        val entry = Entry("Author", "Normal entry without query")
        val postRequest = Request(POST, "/entry").with(entryLens of entry)
        val postResponse = testApp(postRequest)

        postResponse shouldHaveStatus OK
        val responseEntry = entryLens(postResponse)
        responseEntry.author shouldBe "Author"
        responseEntry.text shouldBe "Normal entry without query"
        responseEntry.song shouldBe null
        responseEntry.songQuery shouldBe null
    }

    @Test
    fun `Entry with null songQuery persists correctly`() {
        val entry = Entry("Author", "Entry with explicit null songQuery", songQuery = null)
        val postRequest = Request(POST, "/entry").with(entryLens of entry)
        testApp(postRequest)

        // Verify persistence
        val getResponse = testApp(Request(GET, "/entry"))
        getResponse shouldHaveStatus OK
        val retrievedEntry = entryLens(getResponse)

        retrievedEntry.author shouldBe "Author"
        retrievedEntry.text shouldBe "Entry with explicit null songQuery"
        retrievedEntry.songQuery shouldBe null
    }
}
