package xyz.malefic.daily

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

/**
 * [EntryStorage] is responsible for storing and retrieving entries from a JSON history file.
 *
 * @property storageDir The directory where the entry history file is stored.
 */
class EntryStorage(
    private val storageDir: String = "/data",
) {
    private val historyFile = File("$storageDir/entry_history.json")
    private val nextId = AtomicLong(0)
    private val mapper =
        jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private val entryIdComparator = compareBy<Entry> { it.id }

    private val historyComparator = compareBy<Entry>({ it.date }, { it.id })

    init {
        historyFile.parentFile?.mkdirs()
        if (!historyFile.exists()) {
            historyFile.writeText("[]")
        }
        nextId.set(loadHistory().mapNotNull { it.id }.maxOrNull() ?: 0L)
    }

    /**
     * Saves the given entry to the history.
     * If an entry with the same ID already exists, it is replaced.
     * Otherwise, it is appended.
     *
     * @param entry The entry to be saved.
     */
    @Synchronized
    fun saveEntry(entry: Entry): Entry {
        val history = loadHistory().toMutableList()
        val normalizedEntry = entry.copy(songQuery = null)
        val savedEntry =
            if (normalizedEntry.id == null) {
                normalizedEntry.copy(id = nextId.incrementAndGet())
            } else {
                val numericId = normalizedEntry.id
                nextId.updateAndGet { current -> maxOf(current, numericId) }
                normalizedEntry
            }

        val existingIndex = history.indexOfFirst { it.id == savedEntry.id }

        if (existingIndex >= 0) {
            history[existingIndex] = savedEntry
        } else {
            history.add(savedEntry)
        }

        history.sortWith(historyComparator)
        mapper.writeValue(historyFile, history)
        return savedEntry
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
        loadHistory().takeIf { it.isNotEmpty() }?.let { h -> h.filter { it.date == h.maxOf(Entry::date) }.sortedWith(entryIdComparator) }
            ?: emptyList()

    /**
     * Loads an entry by its ID.
     *
     * @param id The ID of the entry to load.
     * @return The entry with the specified ID, or null if not found.
     */
    fun loadEntry(id: Long): Entry? = loadHistory().firstOrNull { it.id == id }

    /**
     * Loads all entries from a specific date.
     *
     * @param date The date to filter entries by.
     * @return A list of entries from the specified date, sorted by ID for consistency.
     */
    fun loadEntry(date: LocalDate): List<Entry> = loadHistory().filter { it.date == date }.sortedWith(entryIdComparator)

    /**
     * Loads all entries by a specific author.
     *
     * @param author The author to filter entries by.
     * @return A list of entries by the specified author, sorted by ID for consistency.
     */
    fun loadEntry(author: String): List<Entry> = loadHistory().filter { it.author == author }.sortedWith(entryIdComparator)

    /**
     * Deletes an entry by its ID from the history file.
     *
     * @param id The ID of the entry to delete.
     * @return true if an entry was removed, false if no matching entry was found.
     */
    @Synchronized
    fun deleteEntry(id: Long?): Boolean {
        if (id == null) return false
        val history = loadHistory().toMutableList()
        val removed = history.removeIf { it.id == id }
        if (removed) {
            history.sortWith(historyComparator)
            mapper.writeValue(historyFile, history)
        }
        return removed
    }
}
