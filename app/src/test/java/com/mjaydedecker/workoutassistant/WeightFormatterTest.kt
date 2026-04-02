package com.mjaydedecker.workoutassistant

import com.mjaydedecker.workoutassistant.data.model.WeightUnit
import com.mjaydedecker.workoutassistant.util.WeightFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class WeightFormatterTest {

    @Test
    fun `toDisplay returns same value for KG`() {
        assertEquals(80.0, WeightFormatter.toDisplay(80.0, WeightUnit.KG), 0.001)
    }

    @Test
    fun `toDisplay converts kg to lb correctly`() {
        // 1 kg = 2.20462 lb
        assertEquals(2.20462, WeightFormatter.toDisplay(1.0, WeightUnit.LB), 0.001)
        assertEquals(176.37, WeightFormatter.toDisplay(80.0, WeightUnit.LB), 0.01)
    }

    @Test
    fun `toKg is identity for KG`() {
        assertEquals(80.0, WeightFormatter.toKg(80.0, WeightUnit.KG), 0.001)
    }

    @Test
    fun `toKg converts lb back to kg correctly`() {
        assertEquals(1.0, WeightFormatter.toKg(2.20462, WeightUnit.LB), 0.001)
        assertEquals(80.0, WeightFormatter.toKg(176.37, WeightUnit.LB), 0.01)
    }

    @Test
    fun `round-trip kg to lb and back preserves value`() {
        val original = 75.5
        val inLb = WeightFormatter.toDisplay(original, WeightUnit.LB)
        val backToKg = WeightFormatter.toKg(inLb, WeightUnit.LB)
        assertEquals(original, backToKg, 0.001)
    }

    @Test
    fun `format returns one decimal place for KG`() {
        assertEquals("22.5", WeightFormatter.format(22.5, WeightUnit.KG))
    }

    @Test
    fun `format returns one decimal place for LB`() {
        // 22.5 kg = 49.6039 lb → "49.6"
        assertEquals("49.6", WeightFormatter.format(22.5, WeightUnit.LB))
    }

    @Test
    fun `label returns correct unit strings`() {
        assertEquals("kg", WeightFormatter.label(WeightUnit.KG))
        assertEquals("lb", WeightFormatter.label(WeightUnit.LB))
    }
}
