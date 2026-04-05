package xyz.malefic.daily.storage

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import xyz.malefic.daily.format.Quote
import xyz.malefic.daily.format.QuoteWithTimestamp
import java.io.File
import java.time.Instant

/**
 * Class responsible for storing and retrieving quotes from a JSON file.
 *
 * @property storageDir The directory where the quote file is stored.
 */
class QuoteStorage(
    private val storageDir: String = "/data",
) {
    private val file = File("$storageDir/quote.json")
    private val historyFile = File("$storageDir/quote_history.json")
    private val mapper =
        jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    init {
        file.parentFile?.mkdirs()
        if (!file.exists()) {
            saveQuote(Quote("Unknown", "No quote available"))
        }
        if (!historyFile.exists()) {
            historyFile.writeText("[]")
        }
    }

    /**
     * Saves the given quote to the JSON file and appends to history.
     *
     * @param quote The quote to be saved.
     */
    fun saveQuote(quote: Quote) {
        mapper.writeValue(file, quote)
        
        // Append to history
        val history = loadHistory().toMutableList()
        history.add(QuoteWithTimestamp(quote.author, quote.text, Instant.now()))
        mapper.writeValue(historyFile, history)
    }

    /**
     * Loads the quote from the JSON file.
     *
     * @return The loaded quote, or a default quote if the file does not exist.
     */
    fun loadQuote(): Quote =
        if (file.exists()) {
            mapper.readValue(file, Quote::class.java)
        } else {
            Quote("Unknown", "No quote available")
        }

    /**
     * Loads the quote history from the JSON file.
     *
     * @return The list of historical quotes with timestamps.
     */
    fun loadHistory(): List<QuoteWithTimestamp> =
        if (historyFile.exists()) {
            mapper.readValue(historyFile, mapper.typeFactory.constructCollectionType(List::class.java, QuoteWithTimestamp::class.java))
        } else {
            emptyList()
        }
}
