package com.andrea.openrates.data

import kotlin.math.abs

/**
 * Pure conversion math over a [RatesSnapshot]. Kept free of Android and network
 * types so it can be unit tested on the JVM.
 */
object Conversion {

    /**
     * Cross rate for `from -> to`, triangulated through the snapshot base:
     * `rate(A->B) = rate(base->B) / rate(base->A)`.
     *
     * Returns null when either currency is absent from the snapshot.
     */
    fun rate(snapshot: RatesSnapshot, from: String, to: String): Double? {
        if (from == to) return 1.0
        val fromRate = rateFromBase(snapshot, from) ?: return null
        val toRate = rateFromBase(snapshot, to) ?: return null
        if (fromRate == 0.0) return null
        return toRate / fromRate
    }

    fun convert(snapshot: RatesSnapshot, amount: Double, from: String, to: String): Double? =
        rate(snapshot, from, to)?.let { amount * it }

    private fun rateFromBase(snapshot: RatesSnapshot, code: String): Double? =
        if (code == snapshot.base) 1.0 else snapshot.rates[code]

    /**
     * Decimal places for a converted amount: 2 for ordinary currencies, but more
     * when the value is small enough that 2 would round it into noise
     * (e.g. 1 JPY -> 0.0054 EUR).
     */
    fun decimalsFor(value: Double): Int = when {
        value == 0.0 -> 2
        abs(value) >= 1.0 -> 2
        abs(value) >= 0.01 -> 4
        else -> 6
    }
}
