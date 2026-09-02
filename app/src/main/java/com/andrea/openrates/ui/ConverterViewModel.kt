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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConverterUiState(
    val amountInput: String = "1",
    val from: String = "EUR",
    val to: String = "USD",
    val snapshot: RatesSnapshot? = null,
    val names: Map<String, String> = emptyMap(),
    val watchlist: List<String> = emptyList(),
    /** Live per-pair rates keyed by quote code, from the network. Empty when offline. */
    val liveQuotes: Map<String, Double> = emptyMap(),
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val statusMessage: String? = null,
) {
    val amount: Double? get() = parseAmount(amountInput)

    val rate: Double?
        get() = liveQuotes[to] ?: snapshot?.let { Conversion.rate(it, from, to) }

    val converted: Double? get() = rate?.let { r -> amount?.times(r) }

    val hasData: Boolean get() = snapshot != null

    /** Currencies to offer in the picker: the cached snapshot, or a sane starter set. */
    val availableCurrencies: List<String>
        get() = snapshot?.currencies ?: listOf("EUR", "USD", "GBP", "JPY", "CHF")

    fun rateFor(quote: String): Double? =
        liveQuotes[quote] ?: snapshot?.let { Conversion.rate(it, from, quote) }

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

    init {
        viewModelScope.launch {
            // Cached data first so the screen is useful before the network answers.
            val cached = repository.cachedSnapshot()
            val names = repository.cachedNames()
            _state.update {
                it.copy(
                    snapshot = cached ?: it.snapshot,
                    names = names?.names ?: it.names,
                )
            }
            refresh()
        }
    }

    fun onAmountChange(input: String) {
        // Keep digits and a single separator; anything else is a typo, not an amount.
        val filtered = input.filter { it.isDigit() || it == '.' || it == ',' }
        _state.update { it.copy(amountInput = filtered) }
        settings.amount = filtered
    }

    fun onFromChange(code: String) {
        _state.update { it.copy(from = code, liveQuotes = emptyMap()) }
        settings.from = code
        refreshLive()
    }

    fun onToChange(code: String) {
        _state.update { it.copy(to = code, liveQuotes = emptyMap()) }
        settings.to = code
        refreshLive()
    }

    fun onSwap() {
        val current = _state.value
        _state.update { it.copy(from = current.to, to = current.from, liveQuotes = emptyMap()) }
        settings.from = current.to
        settings.to = current.from
        refreshLive()
    }

    fun onToggleWatch(code: String) {
        val updated = _state.value.watchlist.let { list ->
            if (code in list) list - code else list + code
        }
        _state.update { it.copy(watchlist = updated) }
        settings.watchlist = updated
        refreshLive()
    }

    /** Full refresh: snapshot for offline use, plus live values for what is on screen. */
    fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, statusMessage = null) }
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
            fetchLive()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun refreshLive() {
        viewModelScope.launch { fetchLive() }
    }

    /**
     * Uses the single-pair endpoint for the headline conversion and the
     * multi-quote endpoint for the watchlist, so both show exchange-published
     * numbers rather than values triangulated through the snapshot base.
     */
    private suspend fun fetchLive() {
        val current = _state.value
        val pair = repository.livePair(current.from, current.to)
        val quotes = repository.liveQuotes(
            current.from,
            current.watchlist.filter { it != current.from },
        )
        if (pair.isFailure && quotes.isFailure) {
            _state.update {
                it.copy(isOffline = true, statusMessage = offlineMessage(it.snapshot, null))
            }
            return
        }
        val live = buildMap {
            quotes.getOrNull()?.forEach { put(it.quote, it.rate) }
            pair.getOrNull()?.let { put(it.quote, it.rate) }
        }
        _state.update {
            it.copy(liveQuotes = live, isOffline = false, statusMessage = null)
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

        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ConverterViewModel(application) as T
            }
    }
}
