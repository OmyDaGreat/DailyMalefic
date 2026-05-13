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
    private val maxID = AtomicLong(0)
    private val mapper =
        jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private val historyComparator = compareBy<Entry>({ it.date }, { it.id })

    init {
        historyFile.parentFile?.mkdirs()
        if (!historyFile.exists()) {
            historyFile.writeText("[]")
        }

        val history = loadHistory().toMutableList()
        val currentMax = history.mapNotNull { if (it.id > 0L) it.id else null }.maxOrNull() ?: 0L
        maxID.set(currentMax)

        var changed = false
        for (i in history.indices) {
            if (history[i].id == 0L) {
                val newId = maxID.incrementAndGet()
                history[i] = history[i].copy(id = newId)
                changed = true
            }
        }

        if (changed) {
            history.sortWith(historyComparator)
            mapper.writeValue(historyFile, history)
        }
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
            if (normalizedEntry.id == 0L) {
                normalizedEntry.copy(id = maxID.incrementAndGet())
            } else {
                val numericId = normalizedEntry.id
                maxID.updateAndGet { current -> maxOf(current, numericId) }
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
    fun loadLatestDateEntries(): List<Entry> {
        val history = loadHistory()
        val latestDate = history.maxOfOrNull { it.date } ?: return emptyList()
        return history.filter { it.date == latestDate }.sortedBy { it.id }
    }

    /**
     * Loads an entry by its ID.
     *
     * @param id The ID of the entry to load.
     * @return The entry with the specified ID, or null if not found.
     */
    fun loadEntryById(id: Long): Entry? = loadHistory().firstOrNull { it.id == id }

    /**
     * Loads all entries from a specific date.
     *
     * @param date The date to filter entries by.
     * @return A list of entries from the specified date, sorted by ID for consistency.
     */
    fun loadEntriesByDate(date: LocalDate): List<Entry> = loadHistory().filter { it.date == date }.sortedBy { it.id }

    /**
     * Loads all entries by a specific author.
     *
     * @param author The author to filter entries by.
     * @return A list of entries by the specified author, sorted by ID for consistency.
     */
    fun loadEntriesByAuthor(author: String): List<Entry> = loadHistory().filter { it.author == author }.sortedBy { it.id }

    /**
     * Deletes an entry by its ID from the history file.
     *
     * @param id The ID of the entry to delete.
     * @return true if an entry was removed, false if no matching entry was found.
     */
    @Synchronized
    fun deleteEntry(id: Long): Boolean {
        val history = loadHistory().toMutableList()
        val removed = history.removeIf { it.id == id }
        if (removed) {
            history.sortWith(historyComparator)
            mapper.writeValue(historyFile, history)
        }
        return removed
    }
}
