package dev.vladleesi.scadangers.core.security.device

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Strict calendar date; uses API-24-compatible Java APIs without lenient date rollover. */
data class PatchDate private constructor(val isoDate: String, val epochMillis: Long) :
    Comparable<PatchDate> {
    override fun compareTo(other: PatchDate): Int = epochMillis.compareTo(other.epochMillis)

    companion object {
        fun parse(reportedDate: String?): PatchDate? {
            if (reportedDate == null || !DATE.matches(reportedDate)) return null
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val position = ParsePosition(0)
            val parsed = parser.parse(reportedDate, position)
            return if (parsed != null && position.index == reportedDate.length &&
                parser.format(parsed) == reportedDate
            ) {
                PatchDate(reportedDate, parsed.time)
            } else {
                null
            }
        }

        private val DATE = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")
    }
}
