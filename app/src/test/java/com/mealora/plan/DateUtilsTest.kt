package com.mealora.plan

import com.mealora.plan.model.Fallbacks
import com.mealora.plan.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun parsesValidStorageDate() {
        assertEquals(LocalDate.of(2024, 7, 8), DateUtils.parseOrNull("2024-07-08"))
    }

    @Test
    fun invalidDatesReturnNull() {
        assertNull(DateUtils.parseOrNull(null))
        assertNull(DateUtils.parseOrNull(""))
        assertNull(DateUtils.parseOrNull("not-a-date"))
        assertNull(DateUtils.parseOrNull("2024-13-40"))
    }

    @Test
    fun formatFullUsesFallbackForInvalid() {
        assertEquals(Fallbacks.DATE_UNAVAILABLE, DateUtils.formatFull("nope"))
    }

    @Test
    fun storageRoundTrip() {
        val date = LocalDate.of(2024, 12, 31)
        assertEquals(date, DateUtils.parseOrNull(DateUtils.toStorage(date)))
    }
}
