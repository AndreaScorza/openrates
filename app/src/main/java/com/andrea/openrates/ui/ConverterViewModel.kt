package com.andrea.openrates.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.andrea.openrates.data.Conversion
import com.andrea.openrates.data.RatesRepository
import com.andrea.openrates.data.RatesSnapshot
import com.andrea.openrates.data.Settings
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class ConverterUiState(
    val amountInput: String = "1",
    val from: String = "EUR",
    val to: String = "USD",
    /**
     * Every EUR rate in one download. Each pair is crossed from it rather than asked
     * for directly: Frankfurter prints a pair to a few decimals, so from a currency
     * with small units it keeps two or three digits (1M KRW -> GBP comes out 560.00
     * instead of 556.39), while the EUR legs keep five.
     */
    val snapshot: RatesSnapshot? = null,
    val names: Map<String, String> = emptyMap(),
    val watchlist: List<String> = emptyList(),
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val statusMessage: String? = null,
) {
    val amount: Double? get() = parseAmount(amountInput)

    val rate: Double? get() = rateFor(to)

    val converted: Double? get() = rate?.let { r -> amount?.times(r) }

    val hasData: Boolean get() = snapshot != null

    /** Currencies to offer in the picker: the cached snapshot, or a sane starter set. */
    val availableCurrencies: List<String>
        get() = snapshot?.currencies ?: listOf("EUR", "USD", "GBP", "JPY", "CHF")

    fun rateFor(quote: String): Double? = snapshot?.let { Conversion.rate(it, from, quote) }

    /**
     * Swaps the direction and keeps the typed number: 35 EUR -> HKD becomes
     * 35 HKD -> EUR, the usual fix for having picked the pair the wrong way round.
     */
    fun swapped(): ConverterUiState = copy(from = to, to = from)

    companion object {
        /** Accepts both `1,5` and `1.5` so the app works with any keyboard locale. */
        fun parseAmount(input: String): Double? =
            input.replace(',', '.').trim().toDoubleOrNull()
    }
}

class ConverterViewModel(
    application: Application,
    private val repository: RatesRepository = RatesRepository.create(application),
    private val settings: Settings = Settings(application),
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(
        ConverterUiState(
            amountInput = settings.amount,
            from = settings.from,
            to = settings.to,
            watchlist = settings.watchlist,
        )
    )
    val state: StateFlow<ConverterUiState> = _state.asStateFlow()

    /** Cached data first, so the screen is useful before the network answers. */
    private val cacheLoaded = viewModelScope.async {
        val cached = repository.cachedSnapshot()
        val names = repository.cachedNames()
        _state.update {
            it.copy(
                snapshot = cached ?: it.snapshot,
                names = names?.names ?: it.names,
            )
        }
    }

    fun onAmountChange(input: String) {
        // Keep digits and a single separator; anything else is a typo, not an amount.
        val filtered = input.filter { it.isDigit() || it == '.' || it == ',' }
        _state.update { it.copy(amountInput = filtered) }
        settings.amount = filtered
    }

    // Picking, swapping and watching need no network: the snapshot covers every pair.

    fun onFromChange(code: String) {
        _state.update { it.copy(from = code) }
        settings.from = code
    }

    fun onToChange(code: String) {
        _state.update { it.copy(to = code) }
        settings.to = code
    }

    fun onSwap() {
        val swapped = _state.value.swapped()
        _state.value = swapped
        settings.from = swapped.from
        settings.to = swapped.to
    }

    fun onToggleWatch(code: String) {
        val updated = _state.value.watchlist.let { list ->
            if (code in list) list - code else list + code
        }
        _state.update { it.copy(watchlist = updated) }
        settings.watchlist = updated
    }

    /**
     * Called whenever the app comes to the foreground. Rates are published about
     * once a day, so a snapshot under [STALE_AFTER_MS] old is kept as is; the
     * refresh button still forces a download.
     */
    fun refreshIfStale(now: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            cacheLoaded.await()
            val fetchedAt = _state.value.snapshot?.fetchedAtEpochMs
            if (fetchedAt == null || now - fetchedAt >= STALE_AFTER_MS) refresh()
        }
    }

    /** Downloads every EUR rate (one ~10 KB request) and keeps it for offline use. */
    fun refresh() {
        if (_state.value.isRefreshing) return
        // Set before launching, so a second call in the same frame sees it.
        _state.update { it.copy(isRefreshing = true, statusMessage = null) }
        viewModelScope.launch {
            val result = repository.refresh(SNAPSHOT_BASE)
            result.onSuccess { snapshot ->
                _state.update { it.copy(snapshot = snapshot, isOffline = false) }
                if (_state.value.names.isEmpty()) {
                    repository.cachedNames()?.let { names ->
                        _state.update { it.copy(names = names.names) }
                    }
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isOffline = true,
                        statusMessage = offlineMessage(it.snapshot, error.message),
                    )
                }
            }
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun offlineMessage(snapshot: RatesSnapshot?, detail: String?): String = when {
        snapshot == null -> "No rates yet — connect once to download them."
        else -> detail?.takeIf { it.contains("invalid currency", ignoreCase = true) }
            ?: "Offline — showing last saved rates."
    }

    companion object {
        /**
         * Rates are always cached against EUR: Frankfurter's reference base covers
         * every currency in one 10 KB call, and any other pair is derived from it.
         */
        const val SNAPSHOT_BASE = "EUR"

        /** Older than this, coming back to the app downloads fresh rates. */
        val STALE_AFTER_MS = TimeUnit.HOURS.toMillis(1)

        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ConverterViewModel(application) as T
            }
    }
}
