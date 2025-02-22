package xyz.malefic.daily.storage

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import xyz.malefic.daily.format.Quote
import java.io.File

/**
 * Class responsible for storing and retrieving quotes from a JSON file.
 *
 * @property storageDir The directory where the quote file is stored.
 */
class QuoteStorage(
    private val storageDir: String = "/data",
) {
    private val file = File("$storageDir/quote.json")
    private val mapper = jacksonObjectMapper()

    init {
        file.parentFile?.mkdirs()
        if (!file.exists()) {
            saveQuote(Quote("Unknown", "No quote available"))
        }
    }

    /**
     * Saves the given quote to the JSON file.
     *
     * @param quote The quote to be saved.
     */
    fun saveQuote(quote: Quote) {
        mapper.writeValue(file, quote)
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
}
