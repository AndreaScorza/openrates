package com.andrea.openrates.data

import android.content.Context

/** Remembers the user's last pair, amount and watchlist across launches. */
class Settings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("openrates", Context.MODE_PRIVATE)

    var from: String
        get() = prefs.getString(KEY_FROM, "EUR") ?: "EUR"
        set(value) = prefs.edit().putString(KEY_FROM, value).apply()

    var to: String
        get() = prefs.getString(KEY_TO, "USD") ?: "USD"
        set(value) = prefs.edit().putString(KEY_TO, value).apply()

    var amount: String
        get() = prefs.getString(KEY_AMOUNT, "1") ?: "1"
        set(value) = prefs.edit().putString(KEY_AMOUNT, value).apply()

    var watchlist: List<String>
        get() = prefs.getString(KEY_WATCHLIST, DEFAULT_WATCHLIST)
            ?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        set(value) = prefs.edit().putString(KEY_WATCHLIST, value.joinToString(",")).apply()

    private companion object {
        const val KEY_FROM = "from"
        const val KEY_TO = "to"
        const val KEY_AMOUNT = "amount"
        const val KEY_WATCHLIST = "watchlist"
        const val DEFAULT_WATCHLIST = "USD,GBP,JPY,CHF"
    }
}
