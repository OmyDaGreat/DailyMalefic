package xyz.malefic.daily

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.time.LocalDate

/**
 * Class responsible for storing and retrieving entries from a JSON file.
 *
 * @property storageDir The directory where the entry file is stored.
 */
class EntryStorage(
    private val storageDir: String = "/data",
) {
    private val file = File("$storageDir/entry.json")
    private val historyFile = File("$storageDir/entry_history.json")
    private val mapper =
        jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    init {
        file.parentFile?.mkdirs()
        if (!file.exists()) {
            saveEntry(
                Entry(
                    "Unknown",
                    "No entry available",
                    LocalDate.now(),
                ),
            )
        }
        if (!historyFile.exists()) {
            historyFile.writeText("[]")
        }
    }

    /**
     * Saves the given entry to the JSON file and appends to history.
     *
     * @param entry The entry to be saved.
     */
    fun saveEntry(entry: Entry) {
        val persistentEntry = entry.copy(songQuery = null)
        mapper.writeValue(file, persistentEntry)

        val history = loadHistory().toMutableList()
        history.add(persistentEntry)
        mapper.writeValue(historyFile, history)
    }

    /**
     * Loads the entry from the JSON file.
     *
     * @return The loaded entry, or a default if the file does not exist.
     */
    fun loadEntry(): Entry =
        if (file.exists()) {
            mapper.readValue(file, Entry::class.java)
        } else {
            Entry(
                "Unknown",
                "No entry available",
                LocalDate.now(),
            )
        }

    /**
     * Loads the entry history from the JSON file.
     *
     * @return The list of historical entries.
     */
    fun loadHistory(): List<Entry> =
        if (historyFile.exists()) {
            mapper.readValue(historyFile, mapper.typeFactory.constructCollectionType(List::class.java, Entry::class.java))
        } else {
            emptyList()
        }
}
