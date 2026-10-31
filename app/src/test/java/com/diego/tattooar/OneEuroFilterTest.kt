package com.diego.tattooar

import com.diego.tattooar.domain.tracking.OneEuroFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OneEuroFilterTest {

    @Test
    fun testFirstValueReturnsRawValue() {
        val filter = OneEuroFilter()
        val result = filter.filter(100f, 1000L)
        assertEquals(100f, result, 0.001f)
    }

    @Test
    fun testNoiseReductionSmoothesJitter() {
        val filter = OneEuroFilter(minCutoff = 1.0f, beta = 0.007f)
        
        // Initial steady state
        filter.filter(100f, 1000L)
        filter.filter(100f, 1033L)

        // Noisy spike
        val filteredSpike = filter.filter(110f, 1066L)

        // Filtered value should attenuate noisy 10px spike
        assertTrue("Filtered spike should be less than raw spike 110", filteredSpike < 110f)
        assertTrue("Filtered spike should be above baseline 100", filteredSpike > 100f)
    }
}
