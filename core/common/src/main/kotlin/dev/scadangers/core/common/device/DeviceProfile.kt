package dev.scadangers.core.common.device

/**
 * Descriptive device facts as reported by the operating system's build properties.
 *
 * Values are informational: they are not verified, and they are not a security assessment.
 * A `null` field means the platform did not report a usable value. Construct instances with
 * [fromReportedValues] so that OEM-supplied strings are normalized and bounded.
 */
data class DeviceProfile(
    val manufacturer: String?,
    val model: String?,
    val androidRelease: String?,
    val apiLevel: Int,
    val securityPatchLevel: String?
) {
    companion object {
        /** Upper bound for any displayed build property; OEM values are untrusted input. */
        const val MAX_VALUE_LENGTH = 64

        private const val UNKNOWN_BUILD_VALUE = "unknown"

        /**
         * Creates a profile from raw build properties. Blank values and the platform's
         * `unknown` placeholder become `null`; control characters are removed and values
         * longer than [MAX_VALUE_LENGTH] are truncated.
         */
        fun fromReportedValues(
            manufacturer: String?,
            model: String?,
            androidRelease: String?,
            apiLevel: Int,
            securityPatchLevel: String?
        ): DeviceProfile = DeviceProfile(
            manufacturer = normalize(manufacturer),
            model = normalize(model),
            androidRelease = normalize(androidRelease),
            apiLevel = apiLevel,
            securityPatchLevel = normalize(securityPatchLevel)
        )

        internal fun normalize(reportedValue: String?): String? {
            val cleaned = reportedValue
                ?.filterNot { it.isISOControl() }
                ?.trim()
                ?.take(MAX_VALUE_LENGTH)
                ?.trim()
            return cleaned?.takeUnless {
                it.isEmpty() ||
                    it.equals(UNKNOWN_BUILD_VALUE, ignoreCase = true)
            }
        }
    }
}
