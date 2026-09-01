package io.github.hamzatadlaoui.socialgraph

import io.github.hamzatadlaoui.socialgraph.model.FuzzyDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyDateTest {

    private val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )

    @Test
    fun `every shape of half-known date survives a round trip`() {
        val dates = listOf(
            FuzzyDate(1974, 3, 12),
            FuzzyDate(1974, 3),
            FuzzyDate(1974),
            FuzzyDate(1974, approximate = true),
            FuzzyDate(1974, 3, 12, approximate = true),
            FuzzyDate.Unknown,
        )

        for (date in dates) {
            assertEquals(date, FuzzyDate.parse(date.store()))
        }
    }

    @Test
    fun `stores the shapes the brief asks for`() {
        assertEquals("1974-03-12", FuzzyDate(1974, 3, 12).store())
        assertEquals("1974-03", FuzzyDate(1974, 3).store())
        assertEquals("1974", FuzzyDate(1974).store())
        assertEquals("c.1974", FuzzyDate(1974, approximate = true).store())
        assertEquals("", FuzzyDate.Unknown.store())
    }

    @Test
    fun `an unknown date is not a known one`() {
        assertFalse(FuzzyDate.Unknown.isKnown)
        assertTrue(FuzzyDate(1974).isKnown)
    }

    @Test
    fun `nonsense parses as unknown rather than blowing up`() {
        assertEquals(FuzzyDate.Unknown, FuzzyDate.parse(null))
        assertEquals(FuzzyDate.Unknown, FuzzyDate.parse("   "))
        assertEquals(FuzzyDate.Unknown, FuzzyDate.parse("sometime in the eighties"))
        // A month out of range is dropped; the year it came with is kept.
        assertEquals(FuzzyDate(1974), FuzzyDate.parse("1974-13"))
        assertEquals(FuzzyDate(1974, 3), FuzzyDate.parse("1974-03-99"))
    }

    @Test
    fun `reads on screen without inventing precision`() {
        assertEquals("12 March 1974", FuzzyDate(1974, 3, 12).format(months))
        assertEquals("March 1974", FuzzyDate(1974, 3).format(months))
        assertEquals("1974", FuzzyDate(1974).format(months))
        assertEquals("circa 1974", FuzzyDate(1974, approximate = true).format(months))
        assertEquals("Unknown", FuzzyDate.Unknown.format(months))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a month cannot exist without a year`() {
        FuzzyDate(year = null, month = 3)
    }
}
