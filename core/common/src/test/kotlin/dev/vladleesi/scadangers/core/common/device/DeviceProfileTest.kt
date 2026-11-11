package dev.vladleesi.scadangers.core.common.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceProfileTest {
    @Test
    fun keepsReportedValuesTrimmed() {
        val profile = DeviceProfile.fromReportedValues(
            " Example ",
            "Model X",
            "16",
            36,
            "2026-10-05"
        )

        assertEquals("Example", profile.manufacturer)
        assertEquals("Model X", profile.model)
        assertEquals("16", profile.androidRelease)
        assertEquals(36, profile.apiLevel)
        assertEquals("2026-10-05", profile.securityPatchLevel)
    }

    @Test
    fun treatsMissingBlankAndUnknownValuesAsNotReported() {
        assertNull(DeviceProfile.normalize(null))
        assertNull(DeviceProfile.normalize(""))
        assertNull(DeviceProfile.normalize("   "))
        assertNull(DeviceProfile.normalize("unknown"))
        assertNull(DeviceProfile.normalize("UNKNOWN"))
    }

    @Test
    fun removesControlCharacters() {
        assertEquals("ModelX", DeviceProfile.normalize("Model\u0000\nX\u001B"))
        assertNull(DeviceProfile.normalize("\u0007\t\r\n"))
    }

    @Test
    fun boundsOversizedValues() {
        val oversized = "a".repeat(DeviceProfile.MAX_VALUE_LENGTH * 4)

        assertEquals(DeviceProfile.MAX_VALUE_LENGTH, DeviceProfile.normalize(oversized)?.length)
    }
}
