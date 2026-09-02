package com.andrea.openrates.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Disk cache for the last successful download. Writes go to a temp file and are
 * renamed into place, so a kill mid-write can never leave a half-parsed snapshot
 * behind — the app would rather show yesterday's rates than none at all.
 */
class RatesCache(context: Context, directory: File = context.filesDir) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val snapshotFile = File(directory, "rates-snapshot.json")
    private val namesFile = File(directory, "currency-names.json")

    suspend fun readSnapshot(): RatesSnapshot? = read(snapshotFile, RatesSnapshot.serializer())

    suspend fun writeSnapshot(snapshot: RatesSnapshot) =
        write(snapshotFile, RatesSnapshot.serializer(), snapshot)

    suspend fun readNames(): CurrencyNames? = read(namesFile, CurrencyNames.serializer())

    suspend fun writeNames(names: CurrencyNames) =
        write(namesFile, CurrencyNames.serializer(), names)

    private suspend fun <T> read(file: File, serializer: kotlinx.serialization.KSerializer<T>): T? =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!file.exists()) null else json.decodeFromString(serializer, file.readText())
            }.getOrNull()
        }

    private suspend fun <T> write(
        file: File,
        serializer: kotlinx.serialization.KSerializer<T>,
        value: T,
    ) = withContext(Dispatchers.IO) {
        runCatching {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(json.encodeToString(serializer, value))
            tmp.renameTo(file)
        }
        Unit
    }
}
