package xyz.malefic.daily

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.time.LocalDate

/**
 * [EntryStorage] is responsible for storing and retrieving entries from a JSON history file.
 *
 * @property storageDir The directory where the entry history file is stored.
 */
class EntryStorage(
    private val storageDir: String = "/data",
) {
    private val historyFile = File("$storageDir/entry_history.json")
    private val mapper =
        jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    init {
        historyFile.parentFile?.mkdirs()
        if (!historyFile.exists()) {
            historyFile.writeText("[]")
        }
    }

    /**
     * Saves the given entry to the history.
     * If an entry with the same ID already exists, it is replaced.
     * Otherwise, it is appended.
     *
     * @param entry The entry to be saved.
     */
    fun saveEntry(entry: Entry) {
        val newEntry = entry.copy(songQuery = null)

        val history = loadHistory().toMutableList()
        val existingIndex = history.indexOfFirst { it.id == newEntry.id }

        if (existingIndex >= 0) {
            history[existingIndex] = newEntry
        } else {
            history.add(newEntry)
        }

        history.sortBy { it.date }
        mapper.writeValue(historyFile, history)
    }

    /**
     * Loads the full entry history.
     *
     * @return The list of entries.
     */
    fun loadHistory(): List<Entry> =
        if (historyFile.exists()) {
            mapper.readValue(historyFile, mapper.typeFactory.constructCollectionType(List::class.java, Entry::class.java))
        } else {
            emptyList()
        }

    /**
     * Loads all entries from the most recent date.
     *
     * @return A list of entries from the most recent date, sorted by ID for consistency.
     */
    fun loadLatestDateEntries(): List<Entry> {
        val history = loadHistory()
        if (history.isEmpty()) {
            return emptyList()
        }
        val latestDate = history.maxOf { it.date }
        return history.filter { it.date == latestDate }.sortedBy { it.id }
    }
}
