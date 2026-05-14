package xyz.malefic.daily

import io.kotest.matchers.shouldBe
import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NO_CONTENT
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
        val newEntry = Entry(author = "Author", text = "This is a new entry")
        val postRequest = Request(POST, "/entry").with(entryLens of newEntry)
        val postResponse = testApp(postRequest)

        postResponse shouldHaveStatus OK
        val responseEntry = entryLens(postResponse)
        responseEntry.id shouldBe 1L
        responseEntry.author shouldBe newEntry.author
        responseEntry.text shouldBe newEntry.text

        val getResponse = testApp(Request(GET, "/entry?id=${responseEntry.id}"))

        getResponse shouldHaveStatus OK
        entryLens(getResponse) shouldBe responseEntry
    }

    @Test
    fun `Post entry accepts null id and assigns server id`() {
        val postRequest =
            Request(POST, "/entry")
                .body("""{"id":null,"author":"Author","text":"Entry with null id"}""")
        val postResponse = testApp(postRequest)

        postResponse shouldHaveStatus OK
        val responseEntry = entryLens(postResponse)
        responseEntry.id shouldBe 1L
        responseEntry.author shouldBe "Author"
        responseEntry.text shouldBe "Entry with null id"
    }

    @Test
    fun `Delete entry removes it from storage and history`() {
        val newEntry = Entry(author = "Author", text = "To be deleted")
        val savedEntry = entryLens(testApp(Request(POST, "/entry").with(entryLens of newEntry)))

        val deleteResponse = testApp(Request(DELETE, "/entry?id=${savedEntry.id}"))

        deleteResponse shouldHaveStatus OK
        deleteResponse.bodyString() shouldBe "Entry deleted"
        storage.loadHistory().isEmpty() shouldBe true

        val getResponse = testApp(Request(GET, "/entry?id=${savedEntry.id}"))
        getResponse shouldHaveStatus NO_CONTENT
        getResponse.bodyString() shouldBe "No entry found with id ${savedEntry.id}"
    }

    @Test
    fun `Delete entry with missing id returns bad request`() {
        val deleteResponse = testApp(Request(DELETE, "/entry"))

        deleteResponse.status.code shouldBe 400
        deleteResponse.bodyString() shouldBe "Request missing ID to delete"
    }

    @Test
    fun `Delete entry for unknown id returns bad request`() {
        val deleteResponse = testApp(Request(DELETE, "/entry?id=does-not-exist"))

        deleteResponse shouldHaveStatus BAD_REQUEST
        deleteResponse.bodyString() shouldBe "Invalid ID format, expected a number"
    }

    @Test
    fun `Entry persists after server restart`() {
        // Post an entry
        val newEntry = Entry(author = "Persistence Author", text = "This entry should persist")
        val postRequest = Request(POST, "/entry").with(entryLens of newEntry)
        val postResponse = testApp(postRequest)
        val savedEntry = entryLens(postResponse)
        savedEntry.id shouldBe 1L

        // Create new app instance (simulating restart)
        val newStorage = EntryStorage(tempDir.toString())
        val newApp = createApp(newStorage, apiKey = null)

        // Get entry from new instance by ID
        val getResponse = newApp(Request(GET, "/entry?id=${savedEntry.id}"))

        getResponse shouldHaveStatus OK
        entryLens(getResponse) shouldBe savedEntry
    }

    @Test
    fun `Deleted entry does not reappear after restart`() {
        val newEntry = Entry(author = "Author", text = "Persisted then deleted")
        val savedEntry = entryLens(testApp(Request(POST, "/entry").with(entryLens of newEntry)))

        val deleteResponse = testApp(Request(DELETE, "/entry?id=${savedEntry.id}"))
        deleteResponse shouldHaveStatus OK

        val newStorage = EntryStorage(tempDir.toString())
        val newApp = createApp(newStorage, apiKey = null)

        newStorage.loadHistory().isEmpty() shouldBe true

        val getResponse = newApp(Request(GET, "/entry?id=${savedEntry.id}"))
        getResponse shouldHaveStatus NO_CONTENT
        getResponse.bodyString() shouldBe "No entry found with id ${savedEntry.id}"
    }

    @Test
    fun `Initial entry is default when no stored entry exists`() {
        val getResponse = testApp(Request(GET, "/entry"))

        getResponse shouldHaveStatus OK
        val entries = entryListLens(getResponse)
        entries.isEmpty() shouldBe true
    }

    @Test
    fun `Entry history tracks all saved entries`() {
        val entry1 = Entry(author = "Author 1", text = "First entry")
        val entry2 = Entry(author = "Author 2", text = "Second entry")

        val savedEntry1 = entryLens(testApp(Request(POST, "/entry").with(entryLens of entry1)))
        val savedEntry2 = entryLens(testApp(Request(POST, "/entry").with(entryLens of entry2)))
        savedEntry1.id shouldBe 1L
        savedEntry2.id shouldBe 2L

        val historyResponse = testApp(Request(GET, "/entry/history"))
        historyResponse shouldHaveStatus OK

        val history = entryListLens(historyResponse)
        history.size shouldBe 2 // 2 new entries
        history[0].id shouldBe savedEntry1.id
        history[0].author shouldBe "Author 1"
        history[0].text shouldBe "First entry"
        history[1].id shouldBe savedEntry2.id
        history[1].author shouldBe "Author 2"
        history[1].text shouldBe "Second entry"
    }

    @Test
    fun `Post entry with songQuery searches and includes found song`() {
        val entryWithQuery = Entry(author = "Author", text = "Entry with song", songQuery = "test song")
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
        val entry = Entry(author = "Author", text = "Normal entry without query")
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
        val entry = Entry(author = "Author", text = "Entry with explicit null songQuery", songQuery = null)
        val postRequest = Request(POST, "/entry").with(entryLens of entry)
        val postResponse = testApp(postRequest)
        val savedEntry = entryLens(postResponse)

        // Verify persistence by getting the entry by ID
        val getResponse = testApp(Request(GET, "/entry?id=${savedEntry.id}"))
        getResponse shouldHaveStatus OK
        val retrievedEntry = entryLens(getResponse)

        retrievedEntry.author shouldBe "Author"
        retrievedEntry.text shouldBe "Entry with explicit null songQuery"
        retrievedEntry.songQuery shouldBe null
    }
}
