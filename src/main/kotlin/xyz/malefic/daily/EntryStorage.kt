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
    fun loadLatestDateEntries(): List<Entry> =
        loadHistory().takeIf { it.isNotEmpty() }?.let { h -> h.filter { it.date == h.maxOf(Entry::date) }.sortedBy(Entry::id) }
            ?: emptyList()

    /**
     * Loads an entry by its ID.
     *
     * @param id The ID of the entry to load.
     * @return The entry with the specified ID, or null if not found.
     */
    fun loadEntry(id: String): Entry? = loadHistory().firstOrNull { it.id == id }

    /**
     * Loads all entries from a specific date.
     *
     * @param date The date to filter entries by.
     * @return A list of entries from the specified date, sorted by ID for consistency.
     */
    fun loadEntry(date: LocalDate): List<Entry> = loadHistory().filter { it.date == date }.sortedBy(Entry::id)
}
