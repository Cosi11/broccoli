package com.roulette.tracker.data

enum class TimeRange {
    LAST_24H,
    LAST_WEEK,
    LAST_MONTH,
    ALL_TIME;

    fun getDisplayName(): String = when(this) {
        LAST_24H -> "Letzte 24 Stunden"
        LAST_WEEK -> "Letzte Woche"
        LAST_MONTH -> "Letzter Monat"
        ALL_TIME -> "Alle Zeiten"
    }

    fun getMillis(): Long = when(this) {
        LAST_24H -> 24 * 60 * 60 * 1000L
        LAST_WEEK -> 7 * 24 * 60 * 60 * 1000L
        LAST_MONTH -> 30 * 24 * 60 * 60 * 1000L
        ALL_TIME -> 0L
    }
} 