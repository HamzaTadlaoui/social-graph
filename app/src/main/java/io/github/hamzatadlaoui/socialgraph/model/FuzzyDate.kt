package io.github.hamzatadlaoui.socialgraph.model

/**
 * A date that may be only partly known. Section 7 of the brief: people
 * remember "some time in the eighties" far more often than they remember the
 * twelfth of March, and the app must not make them invent the rest.
 *
 * A date is known from the outside in - a [year] alone, then a [month], then a
 * [day] - so a month without a year is not a thing that can exist here.
 */
data class FuzzyDate(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
    val approximate: Boolean = false,
) {
    init {
        require(month == null || year != null) { "a month needs a year" }
        require(day == null || month != null) { "a day needs a month" }
    }

    val isKnown: Boolean get() = year != null

    /**
     * The single string the database column holds: "", "1974", "1974-03",
     * "1974-03-12", each optionally prefixed "c." when [approximate].
     */
    fun store(): String {
        if (year == null) return ""
        val digits = buildString {
            append("%04d".format(year))
            if (month != null) append("-%02d".format(month))
            if (day != null) append("-%02d".format(day))
        }
        return if (approximate) "$CIRCA$digits" else digits
    }

    /**
     * How it reads on screen, given the month names the UI pulled out of
     * resources (January first). Kept here so it can be tested without a device,
     * and so it never reaches for java.time, which minSdk 24 does not have.
     */
    fun format(monthNames: List<String>, unknown: String = "Unknown"): String {
        if (year == null) return unknown
        val month = month?.let { monthNames.getOrNull(it - 1) }
        val plain = when {
            month == null -> "$year"
            day == null -> "$month $year"
            else -> "$day $month $year"
        }
        return if (approximate) "circa $plain" else plain
    }

    companion object {
        /** What an empty field means: nobody has said. */
        val Unknown = FuzzyDate()

        private const val CIRCA = "c."

        /**
         * Reads [store] back. Anything unparseable is treated as unknown rather
         * than thrown away loudly - a date is never worth losing a person over.
         */
        fun parse(stored: String?): FuzzyDate {
            val text = stored?.trim().orEmpty()
            if (text.isEmpty()) return Unknown

            val approximate = text.startsWith(CIRCA)
            val digits = text.removePrefix(CIRCA).trim()
            val parts = digits.split('-')

            val year = parts.getOrNull(0)?.toIntOrNull() ?: return Unknown
            val month = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..12 }
            val day = parts.getOrNull(2)?.toIntOrNull()?.takeIf { month != null && it in 1..31 }

            return FuzzyDate(year, month, day, approximate)
        }
    }
}
