package dev.scadangers

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verify rendered artwork, including gradient and stroke extents, against mask-safe bounds. */
class LauncherIconTest {
    @Test
    fun colorMarkFitsInsideAdaptiveSafeCircle() {
        assertFitsSafeCircle(R.drawable.ic_launcher_foreground)
    }

    @Test
    fun themedLayerIncludesTheCompleteSafePrimaryMark() {
        val primaryPixels = renderPixels(R.drawable.ic_launcher_foreground)
        val themedPixels = renderPixels(R.drawable.ic_launcher_monochrome)
        // The supplied themed layer changes ring alpha and adds full-bleed clouds.
        // The opaque radar/focus features must still be present at the same positions.
        var primaryFeaturePixels = 0
        var missingPixels = 0
        primaryPixels.forEachIndexed { index, color ->
            if (color ushr ALPHA_SHIFT == OPAQUE_ALPHA) {
                primaryFeaturePixels++
                if (themedPixels[index] ushr ALPHA_SHIFT == 0) missingPixels++
            }
        }
        assertTrue("Primary features must be visible", primaryFeaturePixels > MIN_VISIBLE_PIXELS)
        assertEquals("Themed icon must retain the complete radar and focus mark", 0, missingPixels)
    }

    private fun assertFitsSafeCircle(drawableResource: Int) {
        val pixels = renderPixels(drawableResource)
        var visiblePixels = 0
        var clippedPixels = 0
        pixels.forEachIndexed { index, color ->
            if (color ushr ALPHA_SHIFT != 0) {
                visiblePixels++
                val deltaX = index % LAYER_PIXELS - LAYER_PIXELS / 2
                val deltaY = index / LAYER_PIXELS - LAYER_PIXELS / 2
                if (deltaX * deltaX + deltaY * deltaY > SAFE_RADIUS_PIXELS * SAFE_RADIUS_PIXELS) {
                    clippedPixels++
                }
            }
        }
        assertTrue("The launcher mark must be visible", visiblePixels > MIN_VISIBLE_PIXELS)
        assertEquals(
            "Primary artwork extends outside Android's 66 dp safe circle",
            0,
            clippedPixels
        )
    }

    private fun renderPixels(drawableResource: Int): IntArray {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val drawable = checkNotNull(context.getDrawable(drawableResource))
        val bitmap = Bitmap.createBitmap(LAYER_PIXELS, LAYER_PIXELS, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(LAYER_PIXELS * LAYER_PIXELS)
        try {
            drawable.setBounds(0, 0, LAYER_PIXELS, LAYER_PIXELS)
            drawable.draw(Canvas(bitmap))
            bitmap.getPixels(pixels, 0, LAYER_PIXELS, 0, 0, LAYER_PIXELS, LAYER_PIXELS)
        } finally {
            bitmap.recycle()
        }
        return pixels
    }

    private companion object {
        const val LAYER_PIXELS = 432 // 108 dp at 4 pixels per dp.
        const val SAFE_RADIUS_PIXELS = 132 // 66 dp diameter at the same scale.
        const val MIN_VISIBLE_PIXELS = 2000
        const val ALPHA_SHIFT = 24
        const val OPAQUE_ALPHA = 255
    }
}
